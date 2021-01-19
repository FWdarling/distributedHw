
file=dblp.xml
remoteCount=2

lines=$(cat  $file | wc -l)
let n=$remoteCount+1
let line=($lines+2)/$n
split -l $line $file -d $file
 