package org.firstinspires.ftc.teamcode.kernel.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

// Matches annotations by name so this module doesn't depend on the Android TeamCode module.
// Keep the rules in sync with AnnotationBinder, which enforces them again at runtime.
public final class KeyBindingProcessor extends AbstractProcessor {
    static final String PACKAGE = "org.firstinspires.ftc.teamcode.kernel.input.annotations.";
    static final String ON_TOGGLE = PACKAGE + "OnToggle";
    static final String ON_LONG_PRESS = PACKAGE + "OnLongPress";
    static final String ON_DOUBLE_TAP = PACKAGE + "OnDoubleTap";
    static final Set<String> ANNOTATIONS = new LinkedHashSet<>(Arrays.asList(
            PACKAGE + "OnPress", PACKAGE + "OnRelease", PACKAGE + "WhileHeld", ON_TOGGLE, ON_LONG_PRESS, ON_DOUBLE_TAP));

    private static final Set<String> TRIGGER_KEYS = new HashSet<>(Arrays.asList("LEFT_TRIGGER", "RIGHT_TRIGGER"));

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ANNOTATIONS;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        Map<String, Set<String>> bindingsPerClass = new HashMap<>();
        for (TypeElement annotation : annotations) {
            for (Element element : round.getElementsAnnotatedWith(annotation)) {
                check((ExecutableElement) element, mirrorOf(element, annotation), bindingsPerClass);
            }
        }
        return true;
    }

    private static AnnotationMirror mirrorOf(Element element, TypeElement annotation) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().asElement().equals(annotation)) return mirror;
        }
        throw new IllegalStateException("annotation vanished from " + element);
    }

    private void check(ExecutableElement method, AnnotationMirror mirror, Map<String, Set<String>> bindingsPerClass) {
        Binding binding = new Binding(method, mirror, processingEnv.getElementUtils().getElementValuesWithDefaults(mirror));

        if (method.getModifiers().contains(Modifier.STATIC)) {
            error(binding, "must not be static; bindings run on the OpMode instance.");
        }
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            error(binding, "must not be private; make it package-private (drop the 'private').");
        }
        checkParameters(binding);

        if (binding.gamepad != 1 && binding.gamepad != 2) {
            error(binding, "gamepad must be 1 or 2, got " + binding.gamepad + ".");
        }
        if (binding.explicit("threshold")) {
            if (!TRIGGER_KEYS.contains(binding.key)) {
                error(binding, "threshold only applies to LEFT_TRIGGER and RIGHT_TRIGGER, not " + binding.key + ".");
            }
            if (binding.threshold <= 0 || binding.threshold > 1) {
                error(binding, "threshold must be in (0, 1], got " + binding.threshold + ".");
            }
        }
        if (binding.modifiers.contains(binding.key)) {
            error(binding, "lists " + binding.key + " in 'with'; a key can't be its own modifier.");
        }
        checkPositive(binding, ON_LONG_PRESS, "ms");
        checkPositive(binding, ON_DOUBLE_TAP, "windowMs");

        String owner = ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString();
        Set<String> seenInClass = bindingsPerClass.computeIfAbsent(owner, k -> new HashSet<>());
        if (!seenInClass.add(binding.annotationName + "|" + binding.control())) {
            error(binding, "duplicates another @" + binding.annotationName + " for " + binding.control()
                    + " in this class; only one would be useful.");
        }
    }

    private void checkParameters(Binding binding) {
        List<? extends VariableElement> params = binding.method.getParameters();
        if (binding.annotationType.equals(ON_TOGGLE)) {
            if (params.size() != 1 || params.get(0).asType().getKind() != TypeKind.BOOLEAN) {
                error(binding, "must take exactly one boolean parameter (the new toggle state).");
            }
        } else if (!params.isEmpty()) {
            error(binding, "must take no parameters.");
        }
    }

    private void checkPositive(Binding binding, String annotationType, String attribute) {
        if (!binding.annotationType.equals(annotationType)) return;
        long value = ((Number) binding.value(attribute)).longValue();
        if (value <= 0) error(binding, attribute + " must be > 0, got " + value + ".");
    }

    private void error(Binding binding, String problem) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "[kernel] " + binding.describe() + " " + problem, binding.method, binding.mirror);
    }

    private static final class Binding {
        final ExecutableElement method;
        final AnnotationMirror mirror;
        final String annotationType;
        final String annotationName;
        final Map<? extends ExecutableElement, ? extends AnnotationValue> values;
        final String key;
        final int gamepad;
        final double threshold;
        final List<String> modifiers;

        Binding(ExecutableElement method, AnnotationMirror mirror,
                Map<? extends ExecutableElement, ? extends AnnotationValue> values) {
            this.method = method;
            this.mirror = mirror;
            this.values = values;
            TypeElement type = (TypeElement) mirror.getAnnotationType().asElement();
            this.annotationType = type.getQualifiedName().toString();
            this.annotationName = type.getSimpleName().toString();
            this.key = enumName(value("value"));
            this.gamepad = ((Number) value("gamepad")).intValue();
            this.threshold = ((Number) value("threshold")).doubleValue();
            List<String> modifierNames = new ArrayList<>();
            for (Object modifier : (List<?>) value("with")) modifierNames.add(enumName(((AnnotationValue) modifier).getValue()));
            Collections.sort(modifierNames);
            this.modifiers = modifierNames;
        }

        Object value(String attribute) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals(attribute)) return entry.getValue().getValue();
            }
            throw new IllegalStateException(annotationName + " has no attribute " + attribute);
        }

        boolean explicit(String attribute) {
            for (ExecutableElement element : mirror.getElementValues().keySet()) {
                if (element.getSimpleName().contentEquals(attribute)) return true;
            }
            return false;
        }

        private static String enumName(Object value) {
            return ((VariableElement) value).getSimpleName().toString();
        }

        String control() {
            StringBuilder sb = new StringBuilder("gp").append(gamepad).append('.').append(key);
            for (String modifier : modifiers) sb.append('+').append(modifier);
            if (TRIGGER_KEYS.contains(key)) sb.append(">=").append(threshold);
            return sb.toString();
        }

        String describe() {
            return "@" + annotationName + "(" + key + ") " + method.getEnclosingElement().getSimpleName() + "."
                    + method.getSimpleName() + "()";
        }
    }
}
