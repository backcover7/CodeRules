import org.springframework.web.bind.annotation.DeleteMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;

@Component
public class sink1 implements Filter {
    public void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = new HttpServletResponse();

//        response.setHeader("Access-Control-Allow-Origin", "*");

        String hello = req.getParameter("hello");
        javax.naming.Context ctx = new javax.naming.InitialContext();
        ctx.lookup(hello);
    }

    @Path("/message")
    public void test() {

    }
}
