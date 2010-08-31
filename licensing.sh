#!/bin/bash

##
## adds the emarsys copyright and LGPL link to java files if not already present
##
## looks for .java files in the passed source directory
##
## execute this script only from the folder where its located, it won't
## do its job otherwise :(
##
##

## default values

REGEX=copyright
HDR=./copyright_source_header.txt

## get options

while getopts 'h:e:' OPTION ; do
	case $OPTION in
		e)	REGEX=$OPTARG;;
		h)	HDR=$OPTARG;;
		\?)	echo "unknown option \"-$OPTARG\"." >&2
			exit $EXIT_ERROR;;
		:)	echo "Option \"-$OPTARG\" requires an argument!" >&2
			exit $EXIT_ERROR;;
		*)	echo "should not happen!?! ...\"$OPTION\"... " >&2
			exit $EXIT_BUG;;
	esac
done

## get arguments

shift $(( $OPTIND - 1 ))
SRC_DIR=$1

## check options and arguments

if [ ! -d $SRC_DIR ]
then
	echo "invalid source directory $SRC_DIR!"
	exit 1
else 
	echo "source dir: $SRC_DIR"
fi

if [ ! -f $HDR ]
then 
	echo "there's no header file specified!"
	exit 1
else 
	echo "header: $HDR"
fi

## add copyright header to the files in the specified
## folder (recursively) which do not contain a line
## that containing the specified regex

for i in `find $SRC_DIR -iname *.java`
do  
	CR=`grep -iE "$REGEX" $i | wc -l`
	if [ $CR = '0' ] 
	then
		cp $i $i.bak && cat $HDR > $i && cat $i.bak >> $i
		
		if [ $? = '0' ]
		then 
			echo "$i succesfully added copyright note"
		else
			echo "$i failed to copyright note!!!"	
		fi
	else 
		echo "$i already has a copyright note!"
	fi
done
