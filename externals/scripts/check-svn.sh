UPDATE=$1
INDENT=$2
if [[ -z "$1" ]]; then UPDATE="null" ; else UPDATE=$1 ; fi
SVN_PROTOCOL=svn
SVN_URL=178.79.183.218/kavi
FOLDER=`pwd`
PROJECT=`basename $FOLDER`
#echo ${INDENT} Checking svn status for ${PROJECT}
LOCAL_VERSION=`svn info | grep -i 'Last\ Changed\ Rev' | cut -d' ' -f 4`
REPOSITORY_VERSION=`svn info ${SVN_PROTOCOL}://${SVN_URL}/${PROJECT} | grep -i 'Last\ changed\ rev'| cut -d' ' -f 4`
if [ ${LOCAL_VERSION} -eq ${REPOSITORY_VERSION} ] 
then
    echo ${INDENT} No change
else
    echo ${INDENT} Newer version exists in repository
    [[ "${UPDATE}" = "-y" ]] && svn up || echo "Not updating. Specify with -y to automatically update."
fi



