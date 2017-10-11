package guide.example._2_adding_the_first_endpoint

import org.http4k.core.HttpHandler
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun MyMathServer(port: Int): Http4kServer = MyMathsApp().asServer(Jetty(port))

fun MyMathsApp(): HttpHandler = ServerFilters.CatchLensFailure.then(
        routes(
                "/ping" bind GET to { _: Request -> Response(OK) },
                "/add" bind GET to { request: Request ->
                    val valuesToAdd = Query.int().multi.defaulted("value", listOf()).extract(request)
                    Response(OK).body(valuesToAdd.sum().toString())
                }
        )
)
