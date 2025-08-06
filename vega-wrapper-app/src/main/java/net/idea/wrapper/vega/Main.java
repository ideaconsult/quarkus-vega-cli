package net.idea.wrapper.vega;


import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(name = "main", subcommands = {
    WrapperCommand.class
})
public class Main {
}
