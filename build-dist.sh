#!/bin/sh

set -e

VERSION=$(cat project.clj | head -n 1 | awk '{print substr($3, 2, length($3)-2)}')
JAR_NAME="target/dstt-$VERSION-standalone.jar"

echo "Building distribution for $VERSION"

echo "Building uberjar..."
lein uberjar

echo "Writing invocation script..."
mkdir target/dstt-$VERSION
echo "#!/bin/bash" > target/dstt-$VERSION/dstt
echo "" >> target/dstt-$VERSION/dstt
echo "java -jar dstt-$VERSION-standalone.jar \"$@\"" >> target/dstt-$VERSION/dstt
chmod ugo+x target/dstt-$VERSION/dstt

echo "Copying over files..."
cp README.md target/dstt-$VERSION/
cp LICENSE target/dstt-$VERSION/
cp $JAR_NAME target/dstt-$VERSION/

echo "Building xz archive..."
tar cJf target/dstt-$VERSION.tar.xz target/dstt-$VERSION
mv target/dstt-$VERSION.tar.xz .

echo "Distribution built."
