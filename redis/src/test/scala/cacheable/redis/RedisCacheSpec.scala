package cacheable.redis

import org.scalatest.{BeforeAndAfter, ShouldMatchers, FlatSpec}
import scala.concurrent.duration._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Span, Seconds}

import scala.language.postfixOps
import com.redis.RedisClient
import scala.util.{Success, Failure, Try}


/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class RedisCacheSpec extends FlatSpec with ShouldMatchers with Eventually with BeforeAndAfter with RedisSerialization {

  Try(new RedisClient("localhost", 6379)) match {
    case Failure(_) => alert("Skipping tests because Redis does not appear to be running on localhost.")
    case Success(client) => {

      val cache = RedisCache(client)

      before {
        client.del("key1", "key2", "key3", "key4", "key5")
      }

      behavior of "get"

      it should "return the value stored in Redis" in {
        client.set("key1", 123)
        cache.get("key1") should be(Some(123))
      }

      it should "return None if the given key does not exist in the underlying cache" in {
        cache.get("non-existent-key") should be(None)
      }

      behavior of "put"

      it should "store the given key-value pair in the underlying cache" in {
        cache.put("key2", 123, None)
        client.get("key2") should be(Some(123))
      }

      behavior of "put with TTL"

      it should "store the given key-value pair in the underlying cache" in {
        cache.put("key3", 123, Some(1 second))
        client.get("key3") should be(Some(123))

        // Should expire after 1 second
        eventually(timeout(Span(2, Seconds))) {
          client.get("key3") should be(None)
        }
      }

      behavior of "put with TTL of zero"

      it should "store the given key-value pair in the underlying cache with no expiry" in {
        cache.put("key4", 123, Some(Duration.Zero))
        client.get("key4") should be(Some(123))
        client.ttl("key4") should be(Some(-1))
      }

      behavior of "put with TTL of less than 1 second"

      it should "store the given key-value pair in the underlying cache" in {
        cache.put("key5", 123, Some(100 milliseconds))
        client.get("key5") should be(Some(123))
        client.pttl("key5").get should be > 0L

        // Should expire after 1 second
        eventually(timeout(Span(2, Seconds))) {
          client.get("key5") should be(None)
        }
      }

      behavior of "caching with serialization"

      it should "round-trip a String" in {
        cache.put("string", "hello", None)
        cache.get("string") should be(Some("hello"))
      }

      it should "round-trip a byte array" in {
        cache.put("bytearray", "world".getBytes("UTF-8"), None)
        new String(cache.get("bytearray").get, "UTF-8") should be("world")
      }

      it should "round-trip an Int" in {
        cache.put("int", 345, None)
        cache.get("int") should be(Some(345))
      }

      it should "round-trip a Double" in {
        cache.put("double", 1.23, None)
        cache.get("double") should be(Some(1.23))
      }

      it should "round-trip a Long" in {
        cache.put("long", 3456L, None)
        cache.get("long") should be(Some(3456L))
      }

      it should "round-trip a Serializable case class" in {
        val cc = CaseClass(123, "wow")
        cache.put("caseclass", cc, None)
        cache.get("caseclass") should be(Some(cc))
      }

    }

  }

}


