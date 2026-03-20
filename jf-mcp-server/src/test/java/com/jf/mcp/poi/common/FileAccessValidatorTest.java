package com.jf.mcp.poi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileAccessValidatorTest
{
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException
    {
        Path baseDir = Path.of("target", "file-access-validator-tests");
        Files.createDirectories(baseDir);
        tempDir = Files.createTempDirectory(baseDir, "case-");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        if (tempDir == null || !Files.exists(tempDir))
        {
            return;
        }

        try (var paths = Files.walk(tempDir))
        {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try
                {
                    Files.deleteIfExists(path);
                }
                catch (IOException ex)
                {
                    throw new RuntimeException(ex);
                }
            });
        }
    }

    @Test
    void rejectsBlankPath() throws IOException
    {
        FileAccessValidator validator = validatorFor(tempDir.resolve("allowed"));

        assertThrows(IllegalArgumentException.class, () -> validator.validateReadableFile(" "));
    }

    @Test
    void allowsRegularFileInsideAllowedRoot() throws IOException
    {
        Path allowedRoot = tempDir.resolve("allowed");
        Files.createDirectories(allowedRoot);
        Path file = Files.writeString(allowedRoot.resolve("sample.txt"), "ok");
        FileAccessValidator validator = validatorFor(allowedRoot);

        Path validated = validator.validateReadableFile(file.toString());

        assertEquals(file.toRealPath(), validated);
    }

    @Test
    void rejectsDirectPathOutsideAllowedRoot() throws IOException
    {
        Path allowedRoot = tempDir.resolve("allowed");
        Path outsideFile = Files.writeString(tempDir.resolve("outside.txt"), "blocked");
        FileAccessValidator validator = validatorFor(allowedRoot);

        assertThrows(IllegalArgumentException.class, () -> validator.validateReadableFile(outsideFile.toString()));
    }

    @Test
    void rejectsNormalizedTraversalOutsideAllowedRoot() throws IOException
    {
        Path allowedRoot = tempDir.resolve("allowed");
        Files.createDirectories(allowedRoot);
        Path outsideFile = Files.writeString(tempDir.resolve("outside.txt"), "blocked");
        FileAccessValidator validator = validatorFor(allowedRoot);

        String traversedPath = allowedRoot.resolve("..").resolve(outsideFile.getFileName()).toString();

        assertThrows(IllegalArgumentException.class, () -> validator.validateReadableFile(traversedPath));
    }

    @Test
    void rejectsAccessThroughSymlinkedDirectory() throws IOException
    {
        Path allowedRoot = tempDir.resolve("allowed");
        Files.createDirectories(allowedRoot);
        Path outsideDir = Files.createDirectories(tempDir.resolve("outside"));
        Path outsideFile = Files.writeString(outsideDir.resolve("secret.txt"), "blocked");
        Path linkedDir = allowedRoot.resolve("linked");
        createSymbolicLinkOrSkip(linkedDir, outsideDir);
        FileAccessValidator validator = validatorFor(allowedRoot);

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateReadableFile(linkedDir.resolve(outsideFile.getFileName()).toString()));
    }

    @Test
    void rejectsAccessThroughWindowsJunction() throws IOException, InterruptedException
    {
        Assumptions.assumeTrue(isWindows(), "Windows junctions are only available on Windows");

        Path allowedRoot = tempDir.resolve("allowed");
        Files.createDirectories(allowedRoot);
        Path outsideDir = Files.createDirectories(tempDir.resolve("outside"));
        Path outsideFile = Files.writeString(outsideDir.resolve("secret.txt"), "blocked");
        Path linkedDir = allowedRoot.resolve("junction");
        createWindowsJunctionOrSkip(linkedDir, outsideDir);
        FileAccessValidator validator = validatorFor(allowedRoot);

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateReadableFile(linkedDir.resolve(outsideFile.getFileName()).toString()));
    }

    private FileAccessValidator validatorFor(Path allowedRoot) throws IOException
    {
        Files.createDirectories(allowedRoot);

        FileAccessProperties properties = new FileAccessProperties();
        properties.setAllowedRoot(allowedRoot.toString());
        return new FileAccessValidator(properties);
    }

    private void createSymbolicLinkOrSkip(Path link, Path target) throws IOException
    {
        try
        {
            Files.createSymbolicLink(link, target);
        }
        catch (IOException | SecurityException | UnsupportedOperationException ex)
        {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable in this environment: " + ex.getMessage());
        }
    }

    private void createWindowsJunctionOrSkip(Path link, Path target) throws IOException, InterruptedException
    {
        Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString())
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            String output = new String(process.getInputStream().readAllBytes());
            Assumptions.assumeTrue(false, "Windows junctions are unavailable in this environment: " + output);
        }
    }

    private boolean isWindows()
    {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
