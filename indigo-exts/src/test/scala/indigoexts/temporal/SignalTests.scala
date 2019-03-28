package indigoexts.temporal

import utest._
import indigo.GameTime
import indigo.GameTime.Millis

object SignalTests extends TestSuite {

  val tests: Tests =
    Tests {

      "Signals" - {
        "should be able to get a fixed value from a signal" - {
          Signal.fixed("a").at(Millis(100)) ==> "a"
        }

        "should be able to get a value over time from a signal" - {
          val sig = Signal.create(t => t.toInt * 10)

          (0 to 10).foreach { t =>
            sig.at(Millis(t)) ==> t * 10
          }
        }

        "should be able to merge signals" - {
          val a = Signal.fixed(1)
          val b = Signal.fixed(2)

          //Alternative to applicative syntax
          a.merge(b)(_ + _).at(Millis.zero) ==> 3
        }
      }

      "SignalFunctions" - {
        "should be able to compose signal functions" - {
          val f = SignalFunction.lift((i: Int) => s"$i")
          val g = SignalFunction.lift((s: String) => s.length < 2)

          val h: SignalFunction[Int, Boolean] = f andThen g

          h.f(Signal.fixed(1)).at(Millis.zero) ==> true
          h.f(Signal.fixed(1000)).at(Millis.zero) ==> false
        }

        "should be able to run signal functions and parallel" - {
          val f = SignalFunction.lift((i: Int) => s"$i")
          val g = SignalFunction.lift((i: Int) => i < 10)

          val h: SignalFunction[Int, (String, Boolean)] = f and g

          h.f(Signal.fixed(1)).at(Millis.zero) ==> ("1", true)
          h.f(Signal.fixed(1000)).at(Millis.zero) ==> ("1000", false)
        }
      }

      /*
So eventually I'd like a test where you take 3 signals, merge them and prove their bahviour changes like:
Signal(Math.sin + distance)
Signal(math.cos + distance)
Signal(always emits the cut off point)
Signal(TemporalPredicate.until(t >= cut off))

...which is a signal function of Signal[t, a -> Boolean]

Where a thing moves in a circle for 2 seconds and then stops.
       */

      "Moving in a circle" - {

        val distance: Signal[Double] =
          Signal.fixed(10)

        val timeToRadians: Signal[Double] =
          Signal.create(t => (Math.PI * 2) * (1d / 1000d) * (t.toDouble % 1000d))

        val input: Signal[(Double, Double)] =
          Signal.product(timeToRadians, distance)

        val xSignal: SignalFunction[(Double, Double), Int] =
          SignalFunction.lift((t: (Double, Double)) => (Math.sin(t._1) * t._2).toInt)

        val ySignal: SignalFunction[(Double, Double), Int] =
          SignalFunction.lift((t: (Double, Double)) => (Math.cos(t._1) * t._2).toInt)

        val positionSF: SignalFunction[(Double, Double), (Int, Int)] =
          xSignal and ySignal

        val positionSignal: Signal[(Int, Int)] =
          positionSF.f(input)

        positionSignal.at(Millis.zero) ==> (0, 10)
        positionSignal.at(Millis(250)) ==> (10, 0)
        positionSignal.at(Millis(500)) ==> (0, -10)
        positionSignal.at(Millis(750)) ==> (-10, 0)
        positionSignal.at(Millis(1000)) ==> (0, 10)

      }

      "Moving and then stopping after a certain time" - {

        final case class InitialConditions(xPos: Int, velocity: Int, creationTime: Millis, stopAfter: Millis)

        val conditions = InitialConditions(100, 10, Millis.zero, Millis(20000))

        val init: Signal[InitialConditions] =
          Signal.fixed(conditions)

        val timeSignal: Signal[Millis] =
          Signal.create(identity)

        val timeAndConditions: Signal[(Millis, InitialConditions)] =
          Signal.product(timeSignal, init)

        val timeShift: SignalFunction[(Millis, InitialConditions), (Millis, InitialConditions)] =
          SignalFunction.lift(t => (t._1 - t._2.creationTime, t._2))

        val timeStop: SignalFunction[(Millis, InitialConditions), (Millis, InitialConditions)] =
          SignalFunction.lift(t => if(t._1 >= t._2.stopAfter) (t._2.stopAfter, t._2) else t)

        val timeToSeconds: SignalFunction[(Millis, InitialConditions), (Double, InitialConditions)] =
          SignalFunction.lift(t => (t._1.toDouble * 0.001d, t._2))

        val positionX: SignalFunction[(Double, InitialConditions), Int] =
          SignalFunction.lift(t => t._2.xPos + (t._1 * t._2.velocity).toInt)

        val signal: SignalFunction[(Millis, InitialConditions), Int] =
          timeShift andThen timeStop andThen timeToSeconds andThen positionX

        // Sanity check, basic signal should adance position over time
        (0 to 10).toList.foreach { i =>
          signal.f(timeAndConditions).at(Millis(i * 1000)) ==> conditions.xPos + (conditions.velocity * i).toInt
        }

        signal.f(timeAndConditions).at(Millis(30000)) ==> signal.f(timeAndConditions).at(Millis(20000))

      }

    }

}
