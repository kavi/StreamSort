#!/bin/bash
SVN_PROTOCOL=svn
SVN_HOST=178.79.183.218
PROJECT=$1
PROJECT_PATH=../${PROJECT}
TYPE=$2
VISIBILITY=$3
SVN_URL=kavi

SRC_FOLDER=src
TEST_FOLDER=test
DEFAULT_PACKAGE_FOLDER=dk/javacode

if ([ ! -z $3 ] && [ $3 = "dry-run" ]) ; then
  DRYRUN=1
else
  DRYRUN=0
fi

create_project() {
  echo "Creating project folder."
  mkdir $PROJECT || { echo "Unable to create base folder. Exiting." ; exit 1 ; }
}

import_project() {
  echo "Importing project structure to subversion repository."
  echo "Initial import." > svn.msg.tmp
  svn import -F svn.msg.tmp "$PROJECT" "${SVN_PROTOCOL}://${SVN_HOST}/${SVN_URL}/${VISIBILITY}/${PROJECT}"
  rm svn.msg.tmp
  mv "$PROJECT" "${PROJECT}.tmp"
  svn co "${SVN_PROTOCOL}://${SVN_HOST}/${SVN_URL}/${VISIBILITY}/${PROJECT}" "${PROJECT_PATH}"
  echo "Project checked out to ${PROJECT_PATH}"
  echo "Deleting original import."
  rm -rf "${PROJECT}.tmp"
}

# Set svn properties
set_svn_properties() {
  echo "Setting default svn properties."
  echo "ant" > svn.ignore.tmp
  echo "bin" >> svn.ignore.tmp
  echo "lib" >> svn.ignore.tmp
  echo ".project" >> svn.ignore.tmp
  echo ".classpath" >> svn.ignore.tmp
  echo ".settings" >> svn.ignore.tmp
  echo "test-classes" >> svn.ignore.tmp
  svn propset svn:ignore -F svn.ignore.tmp .
  echo "externals ${SVN_PROTOCOL}://${SVN_HOST}/${SVN_URL}/externals" > svn.externals.tmp
  svn propset svn:externals -F svn.externals.tmp .
  svn up
  echo "svn.ignore set to:"
  cat svn.ignore.tmp
  echo "svn.externals set to:"
  cat svn.externals.tmp
  rm svn.ignore.tmp
  rm svn.externals.tmp
  echo "lib" > svn.web.ignore.tmp
  echo "classes" > svn.web.ignore.tmp
  if [ $TYPE == "axis" ] ; then
    svn propset svn:ignore -F svn.web.ignore.tmp WebContent/WEB-INF
  elif [ $TYPE == "gwt" ] ; then
    svn propset svn:ignore -F svn.web.ignore.tmp war/WEB-INF
  fi
  rm svn.web.ignore.tmp
}

# Create build.xml
create_build () {
  echo "Creating build.xml"
  echo "<project name=\"${PROJECT}\" default=\"compile\" xmlns:ivy=\"antlib:org.apache.ivy.ant\">" > build.xml
  echo "  <property name=\"source\" value=\"1.7\"/>" >> build.xml
  echo "  <property name=\"target\" value=\"1.7\"/>" >> build.xml
  if [ $TYPE == "java" ] 
  then
    echo "  <import file=\"externals/java_build.xml\"/>" >> build.xml
  fi
  echo "</project>" >> build.xml
}

# Create ivy.xml
create_ivy() {
echo "Creating ivy.xml"
echo "<ivy-module version=\"1.0\">" > ivy.xml
echo "  <info organisation=\"internal\" module=\"${PROJECT}\" status=\"integration\"/>" >> ivy.xml
echo "  <configurations>" >> ivy.xml
echo "    <conf name=\"compile\"/>" >> ivy.xml
echo "    <conf name=\"runtime\" />" >> ivy.xml
echo "    <conf name=\"sources\"/>" >> ivy.xml
echo "    <conf name=\"javadoc\"/>" >> ivy.xml
echo "    <conf name=\"test\"/>" >> ivy.xml
echo "  </configurations>" >> ivy.xml
echo "  <publications>" >> ivy.xml
echo "    <artifact conf=\"compile,runtime\"/>" >> ivy.xml
echo "    <artifact name=\"${PROJECT}-sources\" type=\"source\" ext=\"jar\" conf=\"sources\" />" >> ivy.xml
echo "  </publications>" >> ivy.xml
echo "  <dependencies defaultconf=\"compile->compile;runtime->runtime;sources->sources\">" >> ivy.xml
echo "    <dependency org=\"junit\" name=\"junit\" rev=\"4.12\" conf=\"test->default;javadoc;sources->sources\"/>" >> ivy.xml
echo "    <dependency org=\"net.sourceforge.cobertura\" name=\"cobertura\" rev=\"1.9.4.1\" conf=\"test->default\"/>" >> ivy.xml
echo "  </dependencies>" >> ivy.xml
echo "</ivy-module>" >> ivy.xml
}

setup_project() {
  mkdir -p "${SRC_FOLDER}/${DEFAULT_PACKAGE_FOLDER}"
  mkdir -p "${TEST_FOLDER}"
}


############ Main ####################

if ([ -z $PROJECT ] || [ -z $TYPE ] || [ -z $VISIBILITY ]) ; then 
  echo "Usage: create-project.sh PROJECT TYPE VISIBILITY"
  echo "  PROJECT - The name of the project."
  echo "  TYPE - The project type - (java - stay tuned for more)"
  echo "  VISIBILITY - public or private"
  exit
fi

if [ $DRYRUN -eq 1 ] ; then
  echo "***** Dry run ******"
  echo "Project code will be generated, but not checked in to subversion."
  echo ""
fi

create_project

if [ ! -d ${PROJECT} ] ; then 
  echo "Unable to find temporary project path."
  exit 1
fi
pushd ${PROJECT}
  echo "Generating project code at: " `pwd`
  create_build
  create_ivy
  setup_project
popd

if [ $DRYRUN -eq 0 ] ; then
  import_project
  if [ ! -d ${PROJECT_PATH} ] ; then 
    echo "Unable to find project path."
    exit 1
  fi
  pushd ${PROJECT_PATH}
    set_svn_properties
    ant eclipse
    ant retrieve
    svn ci -m "Default properties set on project."
  popd
fi
