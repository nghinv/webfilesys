package de.webfilesys.gui.user;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.webfilesys.Category;
import de.webfilesys.Comment;
import de.webfilesys.GeoTag;
import de.webfilesys.MetaInfManager;
import de.webfilesys.WebFileSys;
import de.webfilesys.graphics.VideoThumbnailCreator;
import de.webfilesys.gui.xsl.XslVideoListHandler;

/**
 * @author Frank Hoehnel
 */
public class RenameVideoRequestHandler extends UserRequestHandler {
	protected HttpServletRequest req = null;

	protected HttpServletResponse resp = null;

	private boolean requestIsLocal;

	public RenameVideoRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
			PrintWriter output, String uid, boolean requestIsLocal) {
		super(req, resp, session, output, uid);

		this.req = req;
		this.resp = resp;
		this.requestIsLocal = requestIsLocal;
	}

	protected void process() {
		if (!checkWriteAccess()) {
			return;
		}

		String newFileName = getParameter("newFileName");

		String oldFileName = getParameter("fileName");

		String path = getCwd();

		String oldFilePath = null;

		String newFilePath = null;

		if (path.endsWith(File.separator)) {
			oldFilePath = path + oldFileName;
			newFilePath = path + newFileName;
		} else {
			oldFilePath = path + File.separator + oldFileName;
			newFilePath = path + File.separator + newFileName;
		}

		if (!checkAccess(oldFilePath)) {
			return;
		}

		File source = new File(oldFilePath);

		File dest = new File(newFilePath);

		if ((newFileName.indexOf("..") >= 0) || (!source.renameTo(dest))) {
			output.println("<html>");
			output.println("<head>");
			output.println("<script language=\"javascript\">");

			String errorMsg = insertDoubleBackslash(oldFilePath) + "\\n"
					+ getResource("error.renameFailed", "could not be renamed to") + "\\n"
					+ insertDoubleBackslash(newFilePath);

			output.println("alert('" + errorMsg + "');");

			output.println("window.location.href='/custom/webfilesys/servlet?command=listVideos';");

			output.println("</script>");
			output.println("</head>");
			output.println("</html>");
			output.flush();
			return;
		}

		MetaInfManager metaInfMgr = MetaInfManager.getInstance();

		String description = metaInfMgr.getDescription(oldFilePath);

		if ((description != null) && (description.trim().length() > 0)) {
			metaInfMgr.setDescription(newFilePath, description);
		}

		ArrayList<Category> assignedCategories = metaInfMgr.getListOfCategories(oldFilePath);

		if (assignedCategories != null) {
			for (int i = 0; i < assignedCategories.size(); i++) {
				Category cat = (Category) assignedCategories.get(i);

				metaInfMgr.addCategory(newFilePath, cat);
			}
		}

		GeoTag geoTag = metaInfMgr.getGeoTag(oldFilePath);
		if (geoTag != null) {
			metaInfMgr.setGeoTag(newFilePath, geoTag);
		}

		ArrayList<Comment> comments = metaInfMgr.getListOfComments(oldFilePath);
		if ((comments != null) && (comments.size() > 0)) {
			for (Comment comment : comments) {
				metaInfMgr.addComment(newFilePath, comment);
			}
		}

		if (WebFileSys.getInstance().isReverseFileLinkingEnabled()) {
			metaInfMgr.updateLinksAfterMove(oldFilePath, newFilePath, uid);
		}

		metaInfMgr.removeMetaInf(oldFilePath);

		String thumbnailPath = VideoThumbnailCreator.getThumbnailPath(oldFilePath);

		File thumbnailFile = new File(thumbnailPath);

		if (thumbnailFile.exists()) {
			if (!thumbnailFile.delete()) {
				Logger.getLogger(getClass()).debug("failed to remove video thumbnail file " + thumbnailPath);
			}
		}

		(new XslVideoListHandler(req, resp, session, output, uid, requestIsLocal)).handleRequest();
	}
}
