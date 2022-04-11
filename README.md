# advanced-enforcer-rules

Set of rules for `maven-enforcer-plugin`.

## AdvancedDependencyConvergence

Rule that is similar to `DependencyConvergence` with extra configuration:

* `fail` flag that allows to run the rule in reporting-only mode;
* `excludes` list of artifacts excluded from the validation.

### Usage

```
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-enforcer-plugin</artifactId>
            <version>3.0.0</version>
            <dependencies>
                <dependency>
                    <groupId>com.github.alexey-anufriev</groupId>
                    <artifactId>advanced-enforcer-rules</artifactId>
                    <version>${rules-version}</version>
                </dependency>
            </dependencies>
            <executions>
                <execution>
                    <id>enforce-versions</id>
                    <goals>
                        <goal>enforce</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <advancedDependencyConvergence implementation="com.github.alexeyanufriev.advancedenforcerrules.AdvancedDependencyConvergence">
                                <fail>false</fail>
                                <scopes>
                                    <scope>compile</scope>
                                    <scope>runtime</scope>
                                    <scope>provided</scope>
                                    <!-- <scope>test</scope> -->
                                </scopes>
                                <excludes>
                                    <exclude>org.slf4j:slf4j-api:*</exclude>
                                    <exclude>com.fasterxml.jackson.core:jackson-databind:*</exclude>
                                </excludes>
                            </advancedDependencyConvergence>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
