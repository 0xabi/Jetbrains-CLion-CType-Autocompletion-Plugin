package pythonctype;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import me.martinez.pe.*;
import me.martinez.pe.io.CadesFileStream;
import me.martinez.pe.util.ParseError;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PythonCTypeContributor extends CompletionContributor {

    LinkedList<String> getExports(String pathBinary) throws FileNotFoundException {
        File file = new File(pathBinary);
        LinkedList<String> exports = new LinkedList<>();

        PeImage pe = PeImage.read(new CadesFileStream(file))
                .ifErr(err -> System.err.println("Error parsing PE: " + err))
                .ifOk(val -> {
                    // Print warnings if any
                    for (ParseError warning : val.warnings) {
                        System.out.println("Warning: " + warning);
                    }
                })
                .getOkOrDefault(null);

        if (pe == null) {
            System.out.println("Failed to parse the PE file.");
            return exports;
        }

        pe.exports.ifOk(exp -> {
            for (ExportEntry entry : exp.entries) {

                exports.add(entry.name);
                System.out.println("  " + entry.name);

            }
        }).ifErr(err -> System.out.println("No exports: " + err));

        return exports;
    }
    private String findDllPath(PsiElement element) {
        // Get the entire file text
        PsiFile file = element.getContainingFile();
        String fileText = file.getText();

        // Get the text before the cursor
        int offset = element.getTextOffset();
        String textBeforeCursor = fileText.substring(0, offset);

        // Find "variable = ctypes.CDLL("path")" pattern
        // Look backwards for the variable name before the dot
        Pattern dotPattern = Pattern.compile("(\\w+)\\.$");
        Matcher dotMatcher = dotPattern.matcher(textBeforeCursor);

        String varName = null;
        if (dotMatcher.find()) {
            varName = dotMatcher.group(1);
            System.out.println("Found variable: " + varName);
        }

        if (varName == null) {
            return null;
        }

        // Now find the CDLL assignment for this variable
        // Pattern: varName = ctypes.CDLL("path") or varName = CDLL("path")
        String patternStr = varName + "\\s*=\\s*(?:ctypes\\.)?CDLL\\s*\\(\\s*[\"']([^\"']+)[\"']";
        Pattern cdllPattern = Pattern.compile(patternStr);
        Matcher cdllMatcher = cdllPattern.matcher(fileText);

        if (cdllMatcher.find()) {
            String dllPath = cdllMatcher.group(1);
            System.out.println("Found DLL path: " + dllPath);
            return dllPath;
        }

        return null;
    }

    PythonCTypeContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
                new CompletionProvider<>() {
                    public void addCompletions(@NotNull CompletionParameters parameters,
                                               @NotNull ProcessingContext context,
                                               @NotNull CompletionResultSet resultSet) {
                        PsiElement position = parameters.getPosition();

                        // Find the DLL path from the pattern
                        String dllPath = findDllPath(position);
                        if (dllPath == null) {
                            System.out.println("No DLL path found");
                            return;
                        }

                        System.out.println("Extracting exports from: " + dllPath);

                        try {
                            LinkedList<String> exports = getExports(dllPath);
                            for (String export : exports) {
                                resultSet.addElement(LookupElementBuilder.create(export)
                                        .withTypeText("DLL export")
                                        .withIcon(com.intellij.icons.AllIcons.Nodes.Function));
                            }
                            System.out.println("Added " + exports.size() + " exports");
                        } catch (FileNotFoundException e) {
                            System.out.println("DLL not found: " + e.getMessage());
                        }
                    }
                }
        );
    }
}
