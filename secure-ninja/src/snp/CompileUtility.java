package snp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;

/**
 * Provides static methods for in-memory compilation.
 * Code derived from http://www.java2s.com/Code/Java/JDK-6/CompilingfromMemory.htm
 * @author Edwin Tay(20529864) && Wan Ying Goh(20784663)
 * @version Oct 2013
 */
public class CompileUtility {

    /**
     * The regex pattern for the expected position for where to put licenses within source code.
     */
    private static final String LICENSE_PATTERN = "\\s*[/*].*LICENSE.*[*/]\\s*";
    
    /**
     * The regex pattern for the expected position for where to put passwords within source code.
     */
    private static final String PASSWORD_PATTERN = "\\s*[/*].*PASSWORD.*[*/]\\s*";

    /**
     * Compiling a softwareHouse file. The compilation will protect the resulting classfile
     * if provided a license. Note that the resulting class file is produced in the same directory
     * which the calling class is running in.
     * @param file the file to be compiled.
     * @param className the fully qualified classname
     * @param license the license used to protect class file
     * @return true if compilation is successful, false otherwise. False will be returned if license provided is null.
     */
    public static boolean compileSWHFile(File file, String className, String license) {
        if(license == null) {
            return false;
        }
        Scanner sc = null;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            Log.error("Could not find file: %s", file.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
        
        StringWriter writer = new StringWriter();
        while (sc.hasNextLine()) {
            String s = sc.nextLine();
            writer.write(s+"\n");
            s.trim();
            //
            if (s.matches(LICENSE_PATTERN)) {
                writer.write("private static final String LICENSE_STRING = \"" + license + "\";\n");
                sc.nextLine();
            }
        }
        
        JavaFileObject srcFile = new JavaSourceFromFile(className, writer.toString());
        sc.close();
        return compileJavaFileObject(srcFile);
    }

    /**
     * Compiling a developer file. The compilation will protect the resulting classfile
     * if provided a license. Note that the resulting class file is produced in the same directory
     * which the calling class is running in.
     * @param file the file to be compiled.
     * @param className the fully qualified classname
     * @param license the license used to protect class file
     * @param licenses a map of library to licenses used to protect the class files
     * @param password the password used to protect program
     * @return true if compilation is successful, false otherwise. False will be returned if parameters provided are null.
     */
    public static boolean compileDevFile(File file, String className, Map<String, String> licenses,
            String password) {
        if(password == null || licenses == null) {
            return false;
        }
        Scanner sc = null;
        System.err.println(file.getAbsolutePath());
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            Log.error("Could not find file: %s", file.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
        
        StringWriter writer = new StringWriter();
        while (sc.hasNextLine()) {
            String s = sc.nextLine();
            writer.write(s+"\n");
            s.trim();
            if (s.matches(LICENSE_PATTERN)) {
                Set<String> libnames = licenses.keySet();
                for(String name : libnames) {
                    //if a developer is compiling code with null entries, that's their choice and we 
                    //are not handling that
                    writer.write("LICENSE_MAP.put(\"" + name + "\", \"" + licenses.get(name) +"\");\n");
                }
            } else if (s.matches(PASSWORD_PATTERN)) {
                writer.write("private static final String PASSWORD = \"" + password + "\";\n");
                sc.nextLine();
                // HACK: skip next line as it is: private static final String PASSWORD = "";
            }
        }
        
        JavaFileObject srcFile = new JavaSourceFromFile(className, writer.toString());
        sc.close();
        return compileJavaFileObject(srcFile);
    }

    /**
     * Private method to compile java code on the fly.
     * @param file file to be compiled
     * @return true if compilation is successful. False otherwise.
     */
    private static boolean compileJavaFileObject(JavaFileObject file) {
        
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        // note: resulting class ends up inside the same directory as running Java program
        CompilationTask task = compiler.getTask(null, null, diagnostics, null, null,
                compilationUnits);

        boolean success = task.call();
        for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
            Log.log(diagnostic.getCode());
            Log.log(diagnostic.getKind().toString());
            // HACK: a more robust logging platform would effectively log any type of primitive
            // the string conversion was just to get a string object 
            Log.log(diagnostic.getPosition()+"");
            Log.log(diagnostic.getStartPosition()+"");
            Log.log(diagnostic.getEndPosition()+"");
            Log.log(diagnostic.getSource()+"");
            Log.log(diagnostic.getMessage(null));

        }
        
        return success;
    }

    /**
     * A representation of a Java source file object.
     */
    private static class JavaSourceFromFile extends SimpleJavaFileObject {
        private final String code;
    
        JavaSourceFromFile(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }
    
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
