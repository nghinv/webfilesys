package de.webfilesys.gui.xsl.mobile;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.webfilesys.Constants;
import de.webfilesys.FileComparator;
import de.webfilesys.FileContainer;
import de.webfilesys.FileLinkSelector;
import de.webfilesys.FileSelectionStatus;
import de.webfilesys.MetaInfManager;
import de.webfilesys.graphics.ImageDimensions;
import de.webfilesys.graphics.ImageUtils;
import de.webfilesys.gui.xsl.XslRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UTF8URLEncoder;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class MobileShowImageHandler extends XslRequestHandlerBase
{
    private static final int ADJACENT_PREV = 1;
    private static final int ADJACENT_NEXT = 2;
    
	public MobileShowImageHandler(
			HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
	}
	  
	protected void process()
	{
		String imgPath = getParameter("imgPath");

		if (imgPath != null) 
		{
	        if (!checkAccess(imgPath))
	        {
	            return;
	        }
		} 
		else
		{
		    String beforeParm = req.getParameter("before");
		    
		    if (beforeParm != null)
		    {
	            if (!checkAccess(beforeParm))
	            {
	                return;
	            }
	            
	            imgPath = getAdjacentPicture(beforeParm, ADJACENT_PREV);
		    }
		    else
		    {
	            String afterParm = req.getParameter("after");
	            
	            if (afterParm != null)
	            {
	                if (!checkAccess(afterParm))
	                {
	                    return;
	                }
	                
	                imgPath = getAdjacentPicture(afterParm, ADJACENT_NEXT);
	            }
		    }
		}

		MetaInfManager metaInfMgr = MetaInfManager.getInstance();

		Element imageDataElement = doc.createElement("imageData");
			
		doc.appendChild(imageDataElement);
			
		ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/custom/webfilesys/xsl/mobile/showImage.xsl\"");

		doc.insertBefore(xslRef, imageDataElement);

		XmlUtil.setChildText(imageDataElement, "relativePath", this.getHeadlinePath(imgPath), false);

		String description = metaInfMgr.getDescription(imgPath);

        if ((description != null) && (description.length() > 0))
        {
			XmlUtil.setChildText(imageDataElement, "description", CommonUtils.readyForJavascript(description), true);
        }

		String srcFileName = "/custom/webfilesys/servlet?command=getFile&filePath=" + UTF8URLEncoder.encode(imgPath);
		
		XmlUtil.setChildText(imageDataElement, "imageSource", srcFileName, false);
		
		XmlUtil.setChildText(imageDataElement, "imagePath", UTF8URLEncoder.encode(imgPath));

		int windowWidth = Constants.DEFAULT_SCREEN_WIDTH;
		int windowHeight = Constants.DEFAULT_SCREEN_HEIGHT;
		
		Integer windowWidthSession = (Integer) session.getAttribute("windowWidth");
		
		if (windowWidthSession != null)
		{
			windowWidth = windowWidthSession.intValue();
		}
		else
		{
		    String windowWidthParm = req.getParameter("windowWidth");
		    
		    if (windowWidthParm != null) 
		    {
		        try
		        {
		            windowWidth = Integer.parseInt(windowWidthParm);
		            session.setAttribute("windowWidth", new Integer(windowWidth));
		        }
		        catch (NumberFormatException numEx)
		        {
		        }
		    }
		}
		
		Integer windowHeightSession = (Integer) session.getAttribute("windowHeight");
		
		if (windowHeightSession != null)
		{
			windowHeight = windowHeightSession.intValue();
		}
        else
        {
            String windowHeightParm = req.getParameter("windowHeight");
            
            if (windowHeightParm != null) 
            {
                try
                {
                    windowHeight = Integer.parseInt(windowHeightParm);
                    session.setAttribute("windowHeight", new Integer(windowHeight));
                }
                catch (NumberFormatException numEx)
                {
                }
            }
        }
		
		ImageDimensions scaledDim = ImageUtils.getScaledImageDimensions(imgPath, windowWidth - 4, windowHeight - 4);
		
		XmlUtil.setChildText(imageDataElement, "imageWidth", Integer.toString(scaledDim.getOrigWidth()), false);
		XmlUtil.setChildText(imageDataElement, "imageHeight", Integer.toString(scaledDim.getOrigHeight()), false);

		XmlUtil.setChildText(imageDataElement, "displayWidth", Integer.toString(scaledDim.getWidth()), false);
		XmlUtil.setChildText(imageDataElement, "displayHeight", Integer.toString(scaledDim.getHeight()), false);

		this.processResponse("mobile/showImage.xsl");
    }
	
	private String getAdjacentPicture(String imgPath, int adjacentType)
	{
	    String imgFolderPath = imgPath.substring(0, imgPath.lastIndexOf(File.separatorChar));
	    
        FileLinkSelector fileSelector = new FileLinkSelector(imgFolderPath, FileComparator.SORT_BY_FILENAME);

        FileSelectionStatus selectionStatus = fileSelector.selectFiles(Constants.imgFileMasks, 4096, null, null);

        ArrayList<FileContainer> imageFiles = selectionStatus.getSelectedFiles();

        String prevImg = null;
        
        String nextImg = null;
        
        if (imageFiles != null)
        {
            boolean found = false;
            
            for (int i = 0; (!found) && (i < imageFiles.size()); i++)
            {
                FileContainer fileCont = (FileContainer) imageFiles.get(i);
                
                if (!fileCont.isLink())
                {
                    String picturePath = fileCont.getRealFile().getAbsolutePath();
                    
                    if (adjacentType == ADJACENT_PREV) 
                    {
                        if (picturePath.compareToIgnoreCase(imgPath) < 0) 
                        {
                            prevImg = picturePath;
                        }
                        else
                        {
                            found = true;
                        }
                    }
                    else
                    {
                        if (picturePath.compareToIgnoreCase(imgPath) > 0) 
                        {
                            nextImg = picturePath;
                            found = true;
                        }
                    }
                }
            }
        }

        if (adjacentType == ADJACENT_PREV) 
        {
            if (prevImg != null)
            {
                return prevImg;
            }
            return imgPath;
        }
        
        if (nextImg != null)
        {
            return nextImg;
        }

        return imgPath;
	}
}