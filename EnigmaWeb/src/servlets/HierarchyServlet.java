package servlets;

import WebConstants.Constants;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import users.HierarchyManager;
import utils.ServletUtils;

import java.io.IOException;

@WebServlet(name = "HierarchyServlet", urlPatterns = {"/hierarchyServlet"})
public class HierarchyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        Gson gson;

        String userNameAlly = request.getParameter(Constants.USERNAME);
        String uBoatName =  request.getParameter(Constants.UBOAT_NAME);

        HierarchyManager hierarchyManager = ServletUtils.getHierarchyManager(getServletContext());
        hierarchyManager.connectAllyToUBoat(uBoatName,userNameAlly);
        //getServletContext().getRequestDispatcher("/allyDMServlet").forward(request,response);

        response.setStatus(HttpServletResponse.SC_OK);
        //response.sendRedirect("/EnigmaWeb_Web/allyDMServlet");
       // getServletContext().getRequestDispatcher("/allyDMServlet").forward(request,response);
    }

}
