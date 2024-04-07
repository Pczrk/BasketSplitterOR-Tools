## Approach
Instead of making algorithm with branch and bound approach I used already written tools that use that approach.
## Build:
### Install Libraries:
These libraries have to be installed to work.
```maven
mvn install:install-file -Dfile=lib/LPSOLVESolverPack.jar -DgroupId=com.ocado.libs -DartifactId=LPSOLVESolverPack -Dversion=idk -Dpackaging=jar
```
```maven
mvn install:install-file -Dfile=lib/SCPSolver.jar -DgroupId=com.ocado.libs -DartifactId=SCPSolver -Dversion=idk -Dpackaging=jar
```
### Build
```maven
mvn clean install
```

## Tests
Test can only be run by this maven command.
```maven
mvn test
```