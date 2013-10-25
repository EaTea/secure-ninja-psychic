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

public class CompileUtility {
    private static final String LICENSE_PATTERN = "\\s*[/*].*LICENSE.*[*/]\\s*";
    
    private static final String PASSWORD_PATTERN = "\\s*[/*].*PASSWORD.*[*/]\\s*";

    public static boolean compileSWHFile(File f, String className, String license) {
        Scanner sc = null;
        try {
            sc = new Scanner(f);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        
        StringWriter writer = new StringWriter();
        while (sc.hasNextLine()) {
            String s = sc.nextLine();
            writer.write(s+"\n");
            s.trim();
            if (s.matches(LICENSE_PATTERN)) {
                writer.write("private static final String LICENSE_STRING = \"" + license + "\";\n");
                sc.nextLine();
            }
        }
        
        JavaFileObject file = new JavaSourceFromFile(className, writer.toString());
        return compileJavaFileObject(file);
    }
    
    public static boolean compileDevFile(File f, String className, Map<String, String> licenses,
            String password) {
        Scanner sc = null;
        System.err.println(f.getAbsolutePath());
        try {
            sc = new Scanner(f);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
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
                    writer.write("LICENSE_MAP.put(\"" + name + "\", \"" + licenses.get(name) +"\");\n");
                }
            } else if (s.matches(PASSWORD_PATTERN)) {
                writer.write("private static final String PASSWORD = \"" + password + "\";\n");
                sc.nextLine();
                // HACK: skip next line as it is: private static final String PASSWORD = "";
            }
        }
        
        JavaFileObject file = new JavaSourceFromFile(className, writer.toString());
        return compileJavaFileObject(file);
    }

    private static boolean compileJavaFileObject(JavaFileObject file) {
        
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        
        
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        CompilationTask task = compiler.getTask(null, null, diagnostics, null, null,
                compilationUnits);

        boolean success = task.call();
        for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
            Log.log(diagnostic.getCode());
            Log.log(diagnostic.getKind().toString());
            // HACK: a more robust logging platform would effectively log any type of primitive
            Log.log(diagnostic.getPosition()+"");
            Log.log(diagnostic.getStartPosition()+"");
            Log.log(diagnostic.getEndPosition()+"");
            Log.log(diagnostic.getSource()+"");
            Log.log(diagnostic.getMessage(null));

        }
        return success;
    }
}

class JavaSourceFromFile extends SimpleJavaFileObject {
    final String code;
    final String name;

    JavaSourceFromFile(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                Kind.SOURCE);
        this.name = name.replace('.', '/');
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
