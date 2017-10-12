package guide.example._4_adding_an_external_dependency

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import guide.example._4_adding_an_external_dependency.Matchers.answerShouldBe
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

object Matchers {

    fun Response.answerShouldBe(expected: Int) {
        this shouldMatch hasStatus(OK).and(hasBody(expected.toString()))
    }
}

abstract class RecorderCdc {
    abstract val client: HttpHandler

    @Test
    fun `records answer`() {
        Recorder(client).record(123)
        checkAnswerRecorded()
    }

    open fun checkAnswerRecorded(): Unit {}
}

class RealRecorderTest : RecorderCdc() {
    override val client = SetHostFrom(Uri.of("http://realrecorder")).then(OkHttp())
}

class FakeRecorderHttp : HttpHandler {
    val calls = mutableListOf<Int>()

    private val answer = Path.int().of("answer")

    private val app = CatchLensFailure.then(
            routes(
                    "/{answer}" bind POST to { request -> calls.add(answer.extract(request)); Response(ACCEPTED) }
            )
    )

    override fun invoke(request: Request): Response = app(request)
}

class FakeRecorderTest : RecorderCdc() {
    override val client = FakeRecorderHttp()

    override fun checkAnswerRecorded() {
        client.calls shouldMatch equalTo(listOf(123))
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

    @Test
    fun `all endpoints are mounted correctly multiply`() {
        client(Request(GET, "http://localhost:$port/multiply?value=2&value=4")).answerShouldBe(8)
    }
}

class AddFunctionalTest {
    private val client = MyMathsApp()

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

class MultiplyFunctionalTest {
    private val client = MyMathsApp()

    @Test
    fun `products values together`() {
        client(Request(GET, "/multiply?value=2&value=4")).answerShouldBe(8)
    }

    @Test
    fun `answer is zero when no values`() {
        client(Request(GET, "/multiply")).answerShouldBe(0)
    }

    @Test
    fun `bad request when some values are not numbers`() {
        client(Request(GET, "/multiply?value=1&value=notANumber")) shouldMatch hasStatus(BAD_REQUEST)
    }
}