package io.swagger.codegen.languages.scala;

import io.swagger.codegen.CodegenType;

public class ScalaAkkaHttpServerCodegen extends AbstractScalaCodegen {

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "scala-akka-http-server";
    }

    @Override
    public String getHelp() {
        return "Generates a Scala server library  based on Akka Http.";
    }

    @Override
    public String getArgumentsLocation() {
        return "/arguments/scala-akka-http-server.yaml";
    }


}
