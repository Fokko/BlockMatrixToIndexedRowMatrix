package org.apache.spark.mllib.linalg.distributed

import breeze.linalg.{DenseMatrix => BDM}
import org.apache.spark.mllib.linalg.{DenseMatrix, Matrix}
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

/**
  * Created by Fokko Driesprong on 1/19/16.
  */
class PerformanceTestTest extends FlatSpec with Matchers with BeforeAndAfter {

  val conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("BlockMatrix to IndexedRowMatrix")
  val spark = new SparkContext(conf)

  "More efficient transformationfrom BlockMatrix to IndexedRowMatrix" should "give the same matrix" in {
    // From the offical Spark test-suite
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

    val rowMat = PerformanceTest.toIndexedRowMatrixOptimized(gridBasedMat)
    assert(rowMat.numRows() === m)
    assert(rowMat.numCols() === n)
    assert(rowMat.toBreeze() === gridBasedMat.toBreeze())

  }

}
