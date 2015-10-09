rm bin/ut/distcomp/framework/*.class
echo "Compile"
javac -d bin src/ut/distcomp/framework/*.java
echo "Running"
java -cp bin ut.distcomp.framework.ThreePhaseCommit script1.txt
