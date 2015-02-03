package de.berlios.vch.playlist.web;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.playlist.PlaylistService;
import de.berlios.vch.uri.IVchUriResolveService;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;
import de.berlios.vch.web.servlets.VchHttpServlet;

@Component
public class PlaylistServlet extends VchHttpServlet {

    public static final String PATH = "/playlist";

    @Requires(filter = "(instance.name=vch.web.playlist)")
    private ResourceBundleProvider rbp;

    @Requires
    private LogService logger;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private HttpService httpService;

    @Requires
    private PlaylistService playlistService;

    @Requires
    private IVchUriResolveService uriResolver;

    private BundleContext ctx;

    private ServiceRegistration menuReg;

    public PlaylistServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        HttpSession session = req.getSession();
        session.setMaxInactiveInterval(-1);

        Playlist pl = playlistService.getPlaylist();
        boolean json = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));

        String action = req.getParameter("action");
        if ("add".equalsIgnoreCase(action)) {
            String uri = req.getParameter("uri");
            try {
                IWebPage page = uriResolver.resolve(new URI(uri));
                if (page instanceof IVideoPage) {
                    PlaylistEntry entry = new PlaylistEntry((IVideoPage) page);
                    pl.add(entry);

                    if (json) {
                        resp.setContentType("text/plain");
                        resp.getWriter().println("OK");
                        return;
                    }
                } else {
                    String msg = rbp.getResourceBundle().getString("not_a_video");
                    addNotify(req, new NotifyMessage(TYPE.ERROR, msg));
                    logger.log(LogService.LOG_ERROR, msg);
                    if (json) {
                        error(resp, HttpServletResponse.SC_BAD_REQUEST, msg);
                        return;
                    }
                }
            } catch (Exception e) {
                addNotify(req, new NotifyMessage(TYPE.ERROR, e.getLocalizedMessage()));
                logger.log(LogService.LOG_ERROR, e.getLocalizedMessage(), e);
                if (json) {
                    error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getLocalizedMessage());
                    return;
                }
            }
        } else if ("play".equals(action)) {
            try {
                playlistService.play(pl, null);
                if (json) {
                    resp.setContentType("text/plain");
                    resp.getWriter().println("OK");
                    return;
                }
            } catch (Exception e) {
                addNotify(req, new NotifyMessage(TYPE.ERROR, e.getLocalizedMessage()));
                logger.log(LogService.LOG_ERROR, e.getLocalizedMessage(), e);
                if (json) {
                    String msg = rbp.getResourceBundle().getString("couldnt_start_playlist");
                    msg = MessageFormat.format(msg, e.getLocalizedMessage());
                    error(resp, HttpServletResponse.SC_OK, msg);
                    return;
                }
            }
        } else if ("reorder".equals(action)) {
            String[] order = req.getParameterValues("pe[]");
            Playlist newPl = new Playlist();
            for (int i = 0; i < order.length; i++) {
                String id = order[i];
                for (PlaylistEntry playlistEntry : pl) {
                    if (id.equals(playlistEntry.getId())) {
                        newPl.add(playlistEntry);
                    }
                }
            }
            pl = newPl;
            playlistService.setPlaylist(pl);
            resp.getWriter().println("OK");
            return;
        } else if ("clear".equals(action)) {
            pl.clear();
            if (json) {
                resp.setContentType("text/plain");
                resp.getWriter().println("OK");
                return;
            }
        } else if ("list".equals(action)) {
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print("[");
            for (Iterator<PlaylistEntry> iterator = pl.iterator(); iterator.hasNext();) {
                PlaylistEntry entry = iterator.next();
                resp.getWriter().print(
                        "{\"id\":\"" + entry.getId() + "\",\"title\":\""
                                + entry.getVideo().getTitle().replaceAll("\"", "\\\\\"") + "\"}");
                if (iterator.hasNext()) {
                    resp.getWriter().print(",");
                }
            }
            resp.getWriter().print("]");
            return;
        } else if ("remove".equals(action)) {
            String id = req.getParameter("id");
            for (Iterator<PlaylistEntry> iterator = pl.iterator(); iterator.hasNext();) {
                PlaylistEntry playlistEntry = iterator.next();
                if (id.equals(playlistEntry.getId())) {
                    iterator.remove();
                    resp.getWriter().println("OK");
                    return;
                }
            }

            resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
            resp.setContentType("text/plain; charset=utf-8");
            String msg = rbp.getResourceBundle().getString("entry_not_found");
            resp.getWriter().print(msg);
            return;
        }

        // now display the playlist
        displayPlaylist(req, resp, pl);
    }

    private void displayPlaylist(HttpServletRequest req, HttpServletResponse resp, Playlist pl) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", rbp.getResourceBundle().getString("I18N_PLAYLIST"));
        params.put("ACTION", PATH);
        params.put("PLAYLIST", pl);
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));

        String page = templateLoader.loadTemplate("playlist.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        post(req, resp);
    }

    protected void error(HttpServletResponse res, int code, String msg) throws IOException {
        res.setHeader("Content-Type", "text/plain; charset=utf-8");
        res.setStatus(code);
        res.getWriter().println(msg + "\n");
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        // register playlist servlet
        httpService.registerServlet(PATH, this, null, null);

        // register web interface menu
        IWebMenuEntry menu = new WebMenuEntry(rbp.getResourceBundle().getString("I18N_PLAYLIST"));
        menu.setPreferredPosition(Integer.MIN_VALUE + 300);
        menu.setLinkUri("#");
        SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
        IWebMenuEntry entry = new WebMenuEntry();
        entry.setTitle(rbp.getResourceBundle().getString("I18N_MANAGE"));
        entry.setLinkUri(PlaylistServlet.PATH);
        childs.add(entry);
        menu.setChilds(childs);
        menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
    }

    @Invalidate
    public void stop() {
        // unregister the servlet
        if (httpService != null) {
            httpService.unregister(PATH);
        }

        // unregister the web menu
        if (menuReg != null) {
            menuReg.unregister();
        }
    }
}
