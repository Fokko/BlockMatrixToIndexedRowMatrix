package org.apache.spark.mllib.linalg.distributed

import breeze.linalg.{DenseMatrix => BDM, DenseVector => BDV}
import org.apache.spark.mllib.linalg.{Matrices, Matrix, Vectors}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Fokko Driesprong on 1/19/16.
  */
object PerformanceTest {

  implicit class BlockMatrixImprovements(thisMatrix: BlockMatrix) {
    implicit def toIndexedRowMatrixOptimizedFullrow: IndexedRowMatrix = {
      val numCols = thisMatrix.numCols().toInt
      val rowsPerBlock = thisMatrix.rowsPerBlock
      val colsPerBlock = thisMatrix.colsPerBlock

      val rows = thisMatrix.blocks.map(block => (block._1._1, (block._1._2, block._2)))
        .groupByKey()
        .flatMap { case (row, matricesItr) =>

          val rows = matricesItr.head._2.numRows
          val res = BDM.zeros[Double](rows, numCols)

          matricesItr.foreach { case ((idx: Int, mat: Matrix)) =>
            val offset = colsPerBlock * idx
            res(0 until mat.numRows, offset until offset + mat.numCols) := mat.toBreeze
          }

          (0 until rows).map(idx => new IndexedRow((row * rowsPerBlock) + idx, Vectors.dense(res.t(::, idx).toArray)))
        }

      new IndexedRowMatrix(rows)
    }

    implicit def toIndexedRowMatrixOptimizedBlockrow: IndexedRowMatrix = {
      val numCols = thisMatrix.numCols().toInt
      val rowsPerBlock = thisMatrix.rowsPerBlock
      val colsPerBlock = thisMatrix.colsPerBlock

      val rows = thisMatrix.blocks.flatMap { case ((blockRowIdx, blockColIdx), mat) =>
        val dMat = mat.toBreeze.toDenseMatrix.t
        (0 until mat.numRows).map(rowIds =>
          blockRowIdx * rowsPerBlock + rowIds ->(blockColIdx, dMat(::, rowIds).toDenseVector)
        )
      }.groupByKey().map { case (rowIdx, vectors) =>
        val wholeVector = BDV.zeros[Double](numCols)
        vectors.foreach { case (blockColIdx: Int, vec: BDV[Double]) =>
          val offset = colsPerBlock * blockColIdx
          wholeVector(offset until offset + vec.length) := vec
        }
        new IndexedRow(rowIdx, Vectors.fromBreeze(wholeVector))
      }
      new IndexedRowMatrix(rows)
    }
  }

  val MATRIX_SIZE = 1024
  val MATRIX_NUM = 3
  val samples = 10

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("BlockMatrix to IndexedRowMatrix")

    val spark = new SparkContext(conf)

    val size = (0 until MATRIX_NUM).toArray

    val blocksRdd = spark.parallelize(size).cartesian(spark.parallelize(size)).map(pos => {
      ((pos._1, pos._2), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, BDM.rand(MATRIX_SIZE, MATRIX_SIZE).data))
    })

    val mat = new BlockMatrix(blocksRdd, MATRIX_SIZE, MATRIX_SIZE).cache()

    def compute(callable: () => Long): Long = {
      val start = System.currentTimeMillis()
      assert(callable() == MATRIX_SIZE * MATRIX_NUM)
      System.currentTimeMillis() - start
    }

    val oldRuns = (1 to samples).map(_ => compute(mat.toIndexedRowMatrix().rows.count))
    val newRunsFullrow = (1 to samples).map(_ => compute(mat.toIndexedRowMatrixOptimizedFullrow.rows.count))
    val newRunsBlockrow = (1 to samples).map(_ => compute(mat.toIndexedRowMatrixOptimizedBlockrow.rows.count))

    spark.stop()

    printStatistics("Current situation", oldRuns)
    printStatistics("Improved Fullrow situation", newRunsFullrow)
    printStatistics("Improved Blockrow situation", newRunsBlockrow)
  }


  def printStatistics(name: String, scores: Seq[Long]) = {
    val count = scores.length
    val mean = scores.sum / count
    val devs = scores.map(score => (score - mean) * (score - mean))
    val stddev = Math.sqrt(devs.sum / count)

    println(s"Results for: ${name}")
    println(s"Mean: ${mean} (std.dev. ${stddev})")
    println()
  }

}
