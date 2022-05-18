# Paidy Forex Take-Home Coding Exercise
This is a simple exercise to build a local proxy for getting Currency Exchange Rates from an existing codebase, this
consumes the [One-Frame API](https://hub.docker.com/r/paidyinc/one-frame), supports all currencies from
[ISO 4217](https://www.xe.com/iso4217.php).

# How to run
The simplest way to compile and run is using:

`sbt run`

## Caveats

To support 10,000 successful requests per day with 1 API token I have added a cache that caches requests
for up to 5 minutes.

However, since the addition of new supported currencies there are over 25,000 possible currency pairs making hitting
the OneFrame request limit is still possible, if this proves to be a problem a possible solution would be to request
multiple pairs from OneFrame with a single request and cache those values instead of requesting a single pair per request.

## Remarks
I have taken the liberty of updating all existing libraries to their latest versions that work together. In addition,
I also removed some libraries that ended up not being used.

I used scalafix in order to help update cats effect which make it a breeze.

I decided to keep scalafix with some extra rules to help format and clean up the code.

To make it easier to compile and format the code I added a simple code alias `localBuild` that will run scalafmt and
scalafix prior to compiling the code while not removing the option to simply compile.