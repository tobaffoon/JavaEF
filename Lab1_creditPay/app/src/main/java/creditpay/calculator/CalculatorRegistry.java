package creditpay.calculator;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CalculatorRegistry {
    private static final String PACKAGE_NAME = "creditpay.calculator";
    private static final String CLASS_EXTENSION = ".class";

    public static List<MortgageScheduleCalculator> discoverCalculators() {
        List<MortgageScheduleCalculator> calculators = new ArrayList<>();
        
        try {
            ClassLoader classLoader = CalculatorRegistry.class.getClassLoader();
            URL resource = classLoader.getResource(PACKAGE_NAME.replace(".", "/"));
            
            if (resource == null) {
                throw new RuntimeException("Package " + PACKAGE_NAME + " not found on classpath");
            }
            
            URI uri = resource.toURI();
            Path path = Paths.get(uri);
            
            try (Stream<Path> paths = Files.list(path)) {
                paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(CLASS_EXTENSION))
                    .forEach(classFile -> {
                        try {
                            String className = extractClassName(classFile);
                            Class<?> clazz = classLoader.loadClass(className);
                            
                            if (isConcreteMortgageCalculator(clazz)) {
                                MortgageScheduleCalculator instance =
                                    getCalculatorInstance((Class<? extends MortgageScheduleCalculator>) clazz);
                                calculators.add(instance);
                            }
                        } catch (Exception e) {
                            System.err.println("Warning: Could not load calculator from " + classFile + ": " + e.getMessage());
                        }
                    });
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to discover calculators in package " + PACKAGE_NAME, e);
        }
        
        return calculators;
    }

    private static MortgageScheduleCalculator getCalculatorInstance(Class<? extends MortgageScheduleCalculator> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate calculator: " + clazz.getName(), e);
        }
    }

    /**
     * Checks if a class is a concrete (non-abstract) implementation of MortgageScheduleCalculator.
     */
    private static boolean isConcreteMortgageCalculator(Class<?> clazz) {
        return MortgageScheduleCalculator.class.isAssignableFrom(clazz)
                && !Modifier.isAbstract(clazz.getModifiers())
                && !clazz.equals(MortgageScheduleCalculator.class);
    }

    /**
     * Extracts the fully qualified class name from a .class file path.
     */
    private static String extractClassName(Path classFile) {
        String fileName = classFile.getFileName().toString();
        String className = fileName.substring(0, fileName.length() - CLASS_EXTENSION.length());
        return PACKAGE_NAME + "." + className;
    }
}