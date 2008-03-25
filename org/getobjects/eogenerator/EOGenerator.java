package org.getobjects.eogenerator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOHTMLDynamicElement;
import org.getobjects.appserver.products.WOPackageLinker;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOModel;
import org.getobjects.foundation.UData;


/**
 * Generates <em>template</em> EO's from existing EOModels using EOGenerator.
 *
 * Usually you create subclasses of these templates containing
 * all necessary custom business logic in your project and
 * regenerate the templates in case the model(s) change(s).
 */
public class EOGenerator {

  public static void failWithMessage(String _msg) {
    failWithMessage(_msg, false);
  }
  public static void failWithDisplayingUsage() {
    failWithMessage(null, true);
  }
  public static void failWithMessage(String _msg, boolean _displayUsage) {
    if (_msg != null)
      System.err.println(_msg);
    if (_displayUsage)
      System.err.println("Usage: EOGenerator -model=<path> " +
                         "[-model=<path> ...]" +
                         "[-template=<path>] -output=<path>");
    System.exit(-1);
  }

  public static void runWithModelsOnDirectory(List<EOModel> models,
                                              String srcPath,
                                              String tplPath)
  throws IOException
  {
    /* Setup necessary WO stuff */

    WOPackageLinker linker = new WOPackageLinker(true, null);
    if (tplPath != null)
      linker.linkProjectDirectory(tplPath, EOGenerator.class);
    else
      linker.linkClass(EOGenerator.class);
    linker.linkFramework(WOHTMLDynamicElement.class.getPackage().getName());
    linker.linkFramework(WOApplication.class.getPackage().getName());

    WOResourceManager rm = linker.resourceManager();

    List<String> ctxLanguages = new ArrayList<String>();
    ctxLanguages.add("en");

    for (EOModel model : models) {
      for (EOEntity entity : model.entities()) {

        WOContext ctx = new WOContext(null, null);
        ctx.setLanguages(ctxLanguages);
        EOGeneratorComponent g = new EOGeneratorComponent();
        // this is necessary for finding the component's template
        g.setResourceManager(rm);
        // now render the template using the entity
        g.renderObjectInContext(entity, ctx);

        String packagePath = g.packageName.replace(".", File.separator);
        File   packageDir  = new File(srcPath, packagePath);
        if (packageDir.exists() == false && packageDir.mkdirs() == false) {
          failWithMessage("Couldn't create directory '" + packageDir + "'");
        }

        String templateFileName = g.templateClassName + ".java";
        File   templateFile     = new File(packageDir, templateFileName);
        if (templateFile.exists() == false &&
            templateFile.createNewFile() == false)
        {
          failWithMessage("Couldn't create template file '" +
                          templateFile + "'");
        }
        WOResponse r = ctx.response();
        Exception  e = UData.writeToFile(r.content(), templateFile, false);
        if (e != null)
          failWithMessage("Couldn't write template file: " + e);
        System.out.println("Wrote " + templateFile);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    List<EOModel> models  = new ArrayList<EOModel>();
    String        srcPath = null;
    String        tplPath = null;

    for (String arg : args) {
      if (arg.startsWith("-model=")) {
        String  modelPath = arg.substring("-model=".length());
        URL     mURL      = new URI("file", null, modelPath, null).toURL();
        EOModel model     = EOModel.loadModel(mURL);
        models.add(model);
      }
      else if (arg.startsWith("-output=")) {
        if (srcPath != null)
          failWithMessage("-output argument given more than once");
        srcPath = arg.substring("-output=".length());
      }
      else if (arg.startsWith("-template=")) {
        if (tplPath != null)
          failWithMessage("-template argument given more than once");
      }
    }

    if (models.size() == 0 || srcPath == null) {
      failWithDisplayingUsage();
    }

    File srcDir = new File(srcPath);
    if (srcDir.exists() && srcDir.isDirectory() == false) {
      failWithMessage("Output path is a file?!");
    }
    runWithModelsOnDirectory(models, srcPath, tplPath);
  }
}
