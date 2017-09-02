UPDATE=$1
INDENT=$2
FOLDER=`pwd`
PROJECT=`basename $FOLDER`
svn st &> /dev/null | grep -q "^[ACDM\?\!~]" && echo "${INDENT} Local modifications exist" || echo "${INDENT} No modifications"
