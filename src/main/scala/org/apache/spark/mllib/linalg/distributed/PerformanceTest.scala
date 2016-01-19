package org.apache.spark.mllib.linalg.distributed

import breeze.linalg.DenseMatrix
import org.apache.spark.mllib.linalg.{Matrices, Matrix, Vectors}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Fokko Driesprong on 1/19/16.
  */
object PerformanceTest {

  val MATRIX_SIZE = 1024
  val samples = 10;

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("BlockMatrix to IndexedRowMatrix")
    val spark = new SparkContext(conf)

    val blocks = Seq(
      ((0, 0), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data)),
      ((0, 1), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data)),
      ((0, 2), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data)),
      ((1, 0), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data)),
      ((1, 1), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data)),
      ((1, 2), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data)),
      ((2, 0), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data)),
      ((2, 1), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data)),
      ((2, 2), Matrices.dense(MATRIX_SIZE, MATRIX_SIZE, DenseMatrix.rand(MATRIX_SIZE, MATRIX_SIZE).data))
    )

    val mat = new BlockMatrix(spark.parallelize(blocks), MATRIX_SIZE, MATRIX_SIZE).cache()


    def runOld(): Long = {
      val start = System.currentTimeMillis()

      println(mat.toIndexedRowMatrix().rows.count)


      System.currentTimeMillis() - start
    }

    def runNew(): Long = {
      val start = System.currentTimeMillis()

      println(toIndexedRowMatrixOptimized(mat).rows.count)

      System.currentTimeMillis() - start
    }

    val oldRuns = (1 to samples).map(_ => runOld())
    val newRuns = (1 to samples).map(_ => runNew())
    spark.stop()

    printStatistics("Old", oldRuns)
    printStatistics("New", newRuns)

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


  def toIndexedRowMatrixOptimized(thisMatrix: BlockMatrix): IndexedRowMatrix = {
    val numCols = thisMatrix.numCols().toInt
    val rowsPerBlock = thisMatrix.rowsPerBlock
    val colsPerBlock = thisMatrix.colsPerBlock

    val rows = thisMatrix.blocks.map(block => (block._1._1, (block._1._2, block._2)))
      .groupByKey()
      .flatMap { case (row, matricesItr) =>

        val rows = matricesItr.head._2.numRows
        val res = DenseMatrix.zeros[Double](rows, numCols)

        matricesItr.foreach { case ((idx: Int, mat: Matrix)) =>
          val offset = colsPerBlock * idx
          res(0 until mat.numRows, offset until offset + mat.numCols) := mat.toBreeze
        }

        (0 until rows).map(idx => new IndexedRow((row * rowsPerBlock) + idx, Vectors.dense(res.t(::, idx).toArray)))
      }

    new IndexedRowMatrix(rows)
  }


}
