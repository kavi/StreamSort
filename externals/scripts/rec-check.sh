#!/bin/bash
COMMAND=$1
UPDATE=$2
INDENT=$3
FOLDER=`pwd`
PROJECT=`basename $FOLDER`
[[ -z "${UPDATE}" ]] && UPDATE="-n"
echo $INDENT Performing ${COMMAND} on $PROJECT
externals/scripts/${COMMAND}.sh $UPDATE $INDENT
grep "dependency.*internal" ivy.xml | sed -e "s_.*name=\"\([a-z|A-Z|0-9|\.|\-]*\)\".*_\1_" | while read project
do
  pushd `pwd`/../$project > /dev/null
  externals/scripts/rec-check.sh $COMMAND $UPDATE "${INDENT}--"
  popd > /dev/null
done

