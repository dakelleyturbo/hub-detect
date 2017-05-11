package com.blackducksoftware.integration.hub.packman.packagemanager

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode
import com.blackducksoftware.integration.hub.packman.PackageManagerType
import com.blackducksoftware.integration.hub.packman.PackmanProperties
import com.blackducksoftware.integration.hub.packman.packagemanager.pip.PipPackager
import com.blackducksoftware.integration.hub.packman.packagemanager.pip.PipShowMapParser
import com.blackducksoftware.integration.hub.packman.util.FileFinder
import com.blackducksoftware.integration.hub.packman.util.commands.Command
import com.blackducksoftware.integration.hub.packman.util.commands.CommandRunner
import com.blackducksoftware.integration.hub.packman.util.commands.Executable

@Component
class PipPackageManager extends PackageManager {
    public static final String SETUP_FILENAME = 'setup.py'

    Logger logger = LoggerFactory.getLogger(this.getClass())

    @Autowired
    PipPackager pipPackager

    @Autowired
    FileFinder fileFinder

    @Autowired
    PackmanProperties packmanProperties



    @Value('${packman.pip.createVirtualEnv}')
    boolean createVirtualEnv

    @Value('${packman.pip.pip3}')
    boolean pipThreeOverride

    def executables = [
        pip: ['pip', 'pip.exe'],
        pip3: ['pip3', 'pip3.exe'],
        python: ['python', 'python.exe'],
        python3: ['python3', 'python3.exe'],
        virtualenv: ['virtualenv']]

    def folders = [
        bin: ['bin', 'Scripts']]

    PackageManagerType getPackageManagerType() {
        return PackageManagerType.PIP
    }

    boolean isPackageManagerApplicable(String sourcePath) {
        def foundExectables = fileFinder.findExecutables(executables)
        def foundFiles = fileFinder.findFile(sourcePath, SETUP_FILENAME)
        return foundExectables && foundFiles
    }

    List<DependencyNode> extractDependencyNodes(String sourcePath) {
        if(pipThreeOverride) {
            executables['pip'] = executables['pip3'] + executables['pip']
            executables['python'] = executables['python3'] + executables['python']
        }
        Map<String, Executable> foundExecutables = setupEnvironment(sourcePath, executables)
        return pipPackager.makeDependencyNodes(sourcePath, foundExecutables)
    }

    private  Map<String, Executable> setupEnvironment(String sourcePath, Map<String, List<String>> executables) {
        File sourceDirectory = new File(sourcePath)

        Map<String, String> foundExecutables = fileFinder.findExecutables(executables)
        final String python = foundExecutables['python']
        final String pip = foundExecutables['pip']

        CommandRunner commandRunner = new CommandRunner(logger, sourceDirectory)
        final Command installVirtualenvPackage = new Command(pip, 'install', 'virtualenv')

        if (createVirtualEnv) {
            final File virtualEnv = new File(packmanProperties.getOutputDirectoryPath(), 'blackduck_virtualenv')
            final File virtualEnvBin = getVirtualEnvBin(virtualEnv)

            if (virtualEnvBin && virtualEnv.exists()) {
                logger.info("Found virtual environment:${virtualEnv.getAbsolutePath()}")
            } else {
                commandRunner.execute(installVirtualenvPackage)
                String virtualEnvPackage = getPackageLocation(commandRunner, pip, 'virtualenv')
                def createVirtualEnvCommand = new Command(python, "${virtualEnvPackage}/virtualenv.py", virtualEnv.getAbsolutePath())
                commandRunner.execute(createVirtualEnvCommand)
            }
            foundExecutables = fileFinder.findExecutables(executables, virtualEnvBin.getAbsolutePath())
        }
        return foundExecutables
    }

    private String getVirtualEnvBin(File virtualEnvironmentPath) {
        if(virtualEnvironmentPath.exists() && virtualEnvironmentPath.isDirectory()) {
            Map<String, String> folders = fileFinder.findFolders(this.folders, virtualEnvironmentPath.getAbsolutePath())
            def folder = folders['bin']
            return folders['bin']
        }
        null
    }

    private String getPackageLocation(CommandRunner commandRunner, Executable pip, String packageName) {
        def showVirtualenvPackage = new Command(pip, 'show', 'virtualenv')
        def pipShowResults = commandRunner.executeQuietly(showVirtualenvPackage)
        if(!pipShowResults.hasErrors()) {
            def pipShowParser = new PipShowMapParser(":")
            def map = pipShowParser.parse(pipShowResults.outputStream)
            map['Location']
        }
        logger.info("Failed to find the installed virtalenv package")
        null
    }
}