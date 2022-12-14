package servlets;

import DecryptionManager.DM;
import DecryptionManager.Difficulty;
import EnginePackage.Battlefield;
import EnginePackage.EngineCapabilities;
import WebConstants.Constants;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import users.DMManager;
import users.HierarchyManager;
import users.UserManager;
import utils.ServletUtils;
import utils.SessionUtils;

import java.io.IOException;

@WebServlet(name = "AllyDMServlet", urlPatterns = {"/allyDMServlet"})
public class AllyDMServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String uBoatName = request.getParameter(Constants.UBOAT_NAME);
        String taskSize = request.getParameter(Constants.TASK_SIZE);
        String allyName = SessionUtils.getUsername(request);
        Battlefield battlefield = ServletUtils.getBattlefield(getServletContext(), uBoatName);

        DM dm = new DM(battlefield.getEngine(), battlefield.getLevel(), Integer.parseInt(taskSize));
        DMManager DMManager = ServletUtils.getBattlefield(getServletContext(),uBoatName).getDmManager();
        DMManager.addDM(allyName,dm);
        response.setStatus(HttpServletResponse.SC_OK);

        //request.setAttribute(Constants.CLASS_TYPE,Constants.ALLIES_CLASS);


        //getServletContext().getRequestDispatcher("/readyServlet?"+Constants.CLASS_TYPE+"="+Constants.ALLIES_CLASS).forward(request,response);

        //request.getSession(false).setAttribute(Constants.DM, dm); //TODO CHECK IF TRUE AT THE GET SESSION
        //response.setStatus(HttpServletResponse.SC_OK);
        //response.sendRedirect("/EnigmaWeb_Web/readyServlet");

    }

}
