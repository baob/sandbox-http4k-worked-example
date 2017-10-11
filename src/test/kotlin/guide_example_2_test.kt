package guide.example._2_adding_the_first_endpoint

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.should.shouldMatch
import guide.example._2_adding_the_first_endpoint.Matchers.answerShouldBe
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

object Matchers {

    fun Response.answerShouldBe(expected: Int) {
        this shouldMatch hasStatus(OK).and(hasBody(expected.toString()))
    }
}

class EndToEndTest {
    private val port = Random().nextInt(1000) + 8000
    private val client = OkHttp()
    private val server = MyMathServer(port)

    @Before
    fun setup(): Unit {
        server.start()
    }

    @After
    fun teardown(): Unit {
        server.stop()
    }

    @Test
    fun `all endpoints are mounted correctly ping`() {
        client(Request(GET, "http://localhost:$port/ping")) shouldMatch hasStatus(OK)
    }

    @Test
    fun `all endpoints are mounted correctly add`() {
        client(Request(GET, "http://localhost:$port/add?value=1&value=2")).answerShouldBe(3)
    }
}

class AddFunctionalTest {
    private val client: (Request) -> Response  = MyMathsApp()

    @Test
    fun `adds values together`() {
        client(Request(GET, "/add?value=1&value=2")).answerShouldBe(3)
    }

    @Test
    fun `answer is zero when no values`() {
        client(Request(GET, "/add")).answerShouldBe(0)
    }

    @Test
    fun `bad request when some values are not numbers`() {
        client(Request(GET, "/add?value=1&value=notANumber")) shouldMatch hasStatus(BAD_REQUEST)
    }
}