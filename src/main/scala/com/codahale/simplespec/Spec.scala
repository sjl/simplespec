package com.codahale.simplespec

import org.specs2.mutable.Specification
import org.specs2.execute.Result
import org.specs2.specification.Step
import java.lang.reflect.InvocationTargetException
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
abstract class Spec extends Specification with Discovery {
  def beforeAll() {}
  def afterAll() {}

  def arguments = args()

  override def is = Step(beforeAll()) ^ arguments ^ super.is ^ Step(afterAll())

  {
    val klass = Class.forName(this.getClass.getName.replace("$", ""))
    for ((path, requirements) <- discover(klass).groupBy { _.names }) {
      path.mkString(" ") >> {
        requirements.map { req =>
          req.name in {
            req.evaluate() match {
              case null => success
              case r: Result => r
              case other: Object => {
                try {
                  val result = other.getClass.getMethod("toResult")
                  if (classOf[Result].isAssignableFrom(result.getReturnType)) {
                    result.invoke(other).asInstanceOf[Result]
                  } else pending
                } catch {
                  case e@(_: NoSuchMethodException | _:InvocationTargetException) => pending
                }
              }
              case unknown => pending
            }
          }
        }.head
      }
    }
  }
}

trait BeforeEach {
  def beforeEach() {}
}

trait AfterEach {
  def afterEach() {}
}

trait BeforeAndAfterEach extends BeforeEach with AfterEach
