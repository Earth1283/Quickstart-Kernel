package org.firstinspires.ftc.teamcode.kernel.processor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

// Matches annotations by name so this module doesn't depend on the Android TeamCode module.
// Keep the rules in sync with MemberBinder, which enforces them again at runtime.
public final class KernelMemberProcessor extends AbstractProcessor {
    static final String WATCH = "org.firstinspires.ftc.teamcode.kernel.telemetry.Watch";
    static final String EVERY = "org.firstinspires.ftc.teamcode.kernel.time.Every";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new LinkedHashSet<>(Arrays.asList(WATCH, EVERY));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        Map<String, Set<String>> watchKeysPerClass = new HashMap<>();
        for (TypeElement annotation : annotations) {
            String type = annotation.getQualifiedName().toString();
            for (Element element : round.getElementsAnnotatedWith(annotation)) {
                AnnotationMirror mirror = mirrorOf(element, annotation);
                if (type.equals(WATCH)) checkWatch(element, mirror, watchKeysPerClass);
                else checkEvery((ExecutableElement) element, mirror);
            }
        }
        return true;
    }

    private void checkWatch(Element element, AnnotationMirror mirror, Map<String, Set<String>> watchKeysPerClass) {
        String explicitKey = (String) value(mirror, "value");
        String key = explicitKey.isEmpty() ? element.getSimpleName().toString() : explicitKey;
        String description = "@Watch(" + key + ") " + describe(element);

        if (element.getModifiers().contains(Modifier.STATIC)) {
            error(element, mirror, description + " must not be static; it's read from the bound instance.");
        }
        if (element.getKind() == ElementKind.METHOD) {
            ExecutableElement method = (ExecutableElement) element;
            checkInstanceMethodShape(method, mirror, description);
            if (method.getReturnType().getKind() == TypeKind.VOID) {
                error(element, mirror, description + " must return the value to show.");
            }
        }

        String owner = ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString();
        Set<String> seen = watchKeysPerClass.computeIfAbsent(owner, k -> new HashSet<>());
        if (!seen.add(key)) {
            error(element, mirror, description + " duplicates another @Watch key in this class; only one would show.");
        }
    }

    private void checkEvery(ExecutableElement method, AnnotationMirror mirror) {
        long period = ((Number) value(mirror, "value")).longValue();
        String description = "@Every(" + period + ") " + describe(method);
        if (method.getModifiers().contains(Modifier.STATIC)) {
            error(method, mirror, description + " must not be static; it runs on the bound instance.");
        }
        checkInstanceMethodShape(method, mirror, description);
        if (period <= 0) error(method, mirror, description + " period must be > 0 ms, got " + period + ".");
    }

    private void checkInstanceMethodShape(ExecutableElement method, AnnotationMirror mirror, String description) {
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            error(method, mirror, description + " must not be private; make it package-private (drop the 'private').");
        }
        if (!method.getParameters().isEmpty()) {
            error(method, mirror, description + " must take no parameters.");
        }
    }

    private Object value(AnnotationMirror mirror, String attribute) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                processingEnv.getElementUtils().getElementValuesWithDefaults(mirror);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(attribute)) return entry.getValue().getValue();
        }
        throw new IllegalStateException(mirror + " has no attribute " + attribute);
    }

    private static String describe(Element element) {
        String member = element.getEnclosingElement().getSimpleName() + "." + element.getSimpleName();
        return element.getKind() == ElementKind.METHOD ? member + "()" : member;
    }

    private static AnnotationMirror mirrorOf(Element element, TypeElement annotation) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().asElement().equals(annotation)) return mirror;
        }
        throw new IllegalStateException("annotation vanished from " + element);
    }

    private void error(Element element, AnnotationMirror mirror, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "[kernel] " + message, element, mirror);
    }
}
