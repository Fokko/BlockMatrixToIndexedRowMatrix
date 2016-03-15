package org.apache.spark.mllib.linalg.distributed


import org.apache.spark.mllib.linalg.{DenseMatrix, Matrix}
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

/**
  * Created by Fokko Driesprong on 1/19/16.
  */
class PerformanceTestTest extends FlatSpec with Matchers with BeforeAndAfter {
  import org.apache.spark.mllib.linalg.distributed.PerformanceTest._

  val conf = new SparkConf()
    .setMaster("local[1]")
    .setAppName("BlockMatrix to IndexedRowMatrix")
  val spark = new SparkContext(conf)

  "More efficient transformation from BlockMatrix to IndexedRowMatrix" should "give the same matrix" in {
    // From the official Spark test-suite
    val m = 5
    val n = 4
    val rowPerPart = 2
    val colPerPart = 2
    val numPartitions = 3

    val blocks2: Seq[((Int, Int), Matrix)] = Seq(
      ((0, 0), new DenseMatrix(2, 2, Array(1.0, 0.0, 0.0, 2.0))),
      ((0, 1), new DenseMatrix(2, 2, Array(0.0, 1.0, 0.0, 0.0))),
      ((1, 0), new DenseMatrix(2, 2, Array(3.0, 0.0, 1.0, 1.0))),
      ((1, 1), new DenseMatrix(2, 2, Array(1.0, 2.0, 0.0, 1.0))),
      ((2, 1), new DenseMatrix(1, 2, Array(1.0, 5.0))))

    val gridBasedMat = new BlockMatrix(spark.parallelize(blocks2, numPartitions), rowPerPart, colPerPart)

    val rowMatFullrow = gridBasedMat.toIndexedRowMatrixOptimizedFullrow
    assert(rowMatFullrow.numRows() === m)
    assert(rowMatFullrow.numCols() === n)
    assert(rowMatFullrow.toBreeze() === gridBasedMat.toBreeze())

    val rowMatBlockrow = gridBasedMat.toIndexedRowMatrixOptimizedBlockrow
    assert(rowMatBlockrow.numRows() === m)
    assert(rowMatBlockrow.numCols() === n)
    assert(rowMatBlockrow.toBreeze() === gridBasedMat.toBreeze())
  }

}
