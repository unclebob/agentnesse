package agnesse.fixtures;

import agnesse.markdown.FrontMatter;
import agnesseMain.AgNesseMain;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/** Runs one AgNesse command against a temporary wiki and keeps the JSON it wrote. */
public class CommandRun {
  private final File home;
  private String output = "";

  public CommandRun() throws Exception {
    home = Files.createTempDirectory("agnesse-cli").toFile();
    new File(home, "Wiki").mkdirs();
  }

  public void page(String name, String type, String body) throws Exception {
    File file = new File(new File(home, "Wiki"), name.replace('.', '/') + ".md");
    file.getParentFile().mkdirs();
    String text = body == null ? "" : body.replace("<br>", "\n").replace("<br/>", "\n");
    Files.writeString(file.toPath(), FrontMatter.write(type, List.of(), null, false, text));
  }

  public int exitFor(String command) throws Exception {
    return run(command, null, new File(home, "results.json"));
  }

  public int rerunFailures(String command) throws Exception {
    File failures = new File(home, ".agnesse/last-failures.json");
    return run(command, failures, new File(home, "rerun.json"));
  }

  public boolean outputContains(String fragment) {
    return fragment != null && output.contains(fragment);
  }

  public String output() {
    return output;
  }

  private int run(String command, File failures, File results) throws Exception {
    String[] args = failures == null
      ? new String[] {"-d", home.getPath(), "-r", "Wiki", "-c", command, "-b", results.getPath()}
      : new String[] {"-d", home.getPath(), "-r", "Wiki", "-c", command, "-b", results.getPath(),
        "--rerun-failures", failures.getPath()};
    Integer exit = AgNesseMain.start(args);
    output = results.isFile() ? Files.readString(results.toPath()) : "";
    return exit;
  }
}
