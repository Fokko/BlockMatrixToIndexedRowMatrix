# BlockMatrixToIndexedRowMatrix performance test

Apache Spark BlockMatrix to IndexedRowMatrix conversion performance test.
  
    Results for: Current
    Mean: 18729 (std.dev. 1054.990995222234)
    
    Results for: Improved Fullrow
    Mean: 1662 (std.dev. 301.4614403203169) 11.27 times faster
    
    Results for: Improved Blockrow
    Mean: 1162 (std.dev. 114.01315713548152) 16.18 times faster

The first iteration 'Improved Fullrow' provided an increment of 11 times for a dense matrix. The performance gain is obtained by shuffling chuncks of the vector instead of individual values of the matrix around the cluster. The second iteration (Improved Blockrow) is proven to be 16 times faster. The second shuffles the rows of the blocks arround, and them consolidates them into a single vector which is then converted to an IndexedRow.

If you don't trust the numbers, you can execute the test yourself by invoking `sbt run`. Also, tests are included, which has been copied from the original Spark repository and can be executed using `sbt test`.

    Java HotSpot(TM) 64-Bit Server VM warning: ignoring option PermSize=128M; support was removed in 8.0
    Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize=1g; support was removed in 8.0
    [info] BlockMatrixSuite:
    [info] - size (18 milliseconds)
    [info] - grid partitioner (5 milliseconds)
    [info] - toCoordinateMatrix (86 milliseconds)
    [info] - toIndexedRowMatrix (579 milliseconds)
    [info] - toBreeze and toLocalMatrix (33 milliseconds)
    [info] - add (304 milliseconds)
    [info] - multiply (601 milliseconds)
    [info] - simulate multiply (52 milliseconds)
    [info] - validate (291 milliseconds)
    [info] - transpose (62 milliseconds)
    [info] ScalaTest
    [info] Run completed in 6 seconds, 288 milliseconds.
    [info] Total number of tests run: 10
    [info] Suites: completed 1, aborted 0
    [info] Tests: succeeded 10, failed 0, canceled 0, ignored 0, pending 0
    [info] All tests passed.
    [info] Passed: Total 10, Failed 0, Errors 0, Passed 10
    [success] Total time: 21 s, completed Jan 19, 2016 10:38:59 PM
