# BlockMatrixToIndexedRowMatrix performance test

Apache Spark BlockMatrix to IndexedRowMatrix conversion performance test.

  Results for: Current situation
  Mean: 18517 (std.dev. 392.0471910369975)

  Results for: Improved situation
  Mean: 978 (std.dev. 122.72734006732159)

Which is an improvement of almost 19 times for a dense matrix. The performance gain is mostly by sending (parts of) the vector instead of individual elements.

If you don't trust the numbers, you can execute the test yourself by invoking `sbt run`.

A test is included which has been copied from the original Spark repository and can be executed using `sbt test`.

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
