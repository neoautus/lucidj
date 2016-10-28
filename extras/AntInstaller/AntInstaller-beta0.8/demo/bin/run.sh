#!/bin/sh

if [ "$JAVA_HOME" = "" ] ; then
	echo set JAVA_HOME;
	exit 1;
fi

# find the command called's root  e.g.  ./build/
dir=`expr  match "$0" '\(.*\)run.sh'`
echo command called $0 directory 

#  if it we did not call ./  change to the directory we called 
if [ "$dir" != "./" ] ; then
        if [ "$dir" != "" ] ;  then
								echo changing to $dir
                cd "$dir" ;
        fi
fi
CLASSPATH=$CLASSPATH:./classes

echo $CLASSPATH
echo $JAVA_HOME

$JAVA_HOME/bin/java -cp $CLASSPATH org.tp23.demo.Demonstration

 
