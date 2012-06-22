eventCompileStart = { 
  projectCompiler.srcDirectories << "$basedir/injected-src/controllers"
  projectCompiler.srcDirectories << "$basedir/injected-src/realms" 
} 
eventAllTestsStart = { 
  classLoader.addURL(new File("$basedir/injected-src/controllers").toURL()) 
   classLoader.addURL(new File("$basedir/injected-src/realms").toURL()) 
} 