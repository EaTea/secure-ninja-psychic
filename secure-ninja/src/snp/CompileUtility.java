package snp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;

public class CompileUtility {

    public static boolean compileDevAuth(File f, Map<String, String> licenses) {
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
            if (s.indexOf(""))
            writer.write(s);
        }
        
        String fileName = f.getName();
        JavaFileObject file = new JavaSourceFromFile(fileName, writer.toString());
        return compileJavaFileObject(file);
    }
    
    public static boolean compile(File f) {
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
            writer.write(s);
        }
        
        String fileName = f.getName();
        JavaFileObject file = new JavaSourceFromFile(fileName, writer.toString());
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
            System.out.println(diagnostic.getCode());
            System.out.println(diagnostic.getKind());
            System.out.println(diagnostic.getPosition());
            System.out.println(diagnostic.getStartPosition());
            System.out.println(diagnostic.getEndPosition());
            System.out.println(diagnostic.getSource());
            System.out.println(diagnostic.getMessage(null));

        }
        return success;
    }
}

class JavaSourceFromFile extends SimpleJavaFileObject {
    final String code;

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
