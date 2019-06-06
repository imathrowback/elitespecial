# elitespecial
Find "special" bodies in Elite Dangerous journal scan logs

# What is this?

It is a little tool I wrote in Java to tell me if I scanned objects that I considered "interesting"

It continuously watches your journals and shows an entry if it matches one of the rules below.

# What kind of things are interesting?

It's rather subjective, but here is my list of things that it uses that are interesting to me:

* Has atmosphere and is landable (this shouldn't happen, but it sure would be interesting!)
* Is landable and terraformable
* Has gravity higher than 3 and is landable
* Is "close" to it's parent (defined as less than 0.5 light seconds away)
* Has rings that are wider than 1 light second
* bodies on the inner side of, actually INSIDE of, or less than 300km to a ring
* Moon of a moon
* greater than 10,000,000 surface pressure
* Surface temp > 1200K and is landable
* Rotates faster than 0.3 days and is NOT tidally locked
* Landable and small < 300km
* High eccentricity > 0.5 AND is landable AND completes an orbit in less than 7 days
* High eccentricity > 0.9
* Orbits in less than 0.2 days

# Can I change the detection rules?

Not at this time, perhaps in a future version.

# How can I run it?

* [Get Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) and install it - You may already have it installed
* Get the EXE file from the releases page and run it

* If you don't want to *install* the JRE, you can get the tar.gz instead from the Java website and extract it to a folder called "jre" wherever the elitespecial.exe file is


# How can I trust you/this to not break my computer?

You shouldn't trust anything on the internet, especially some random stranger telling you to download stuff.

But if you look through my history, [other tools I've written](https://www.reddit.com/r/Rift/comments/4gzw4g/extracting_game_model_files_and_textures/),  and [the tool](https://github.com/imathrowback/telarafly) I wrote that was [featured on a game developers site](https://www.trionworlds.com/rift/en/2017/06/20/introducing-telarafly-by-ghar-station/), I think I am pretty trustworthy.


# Your code is terrible!

Thanks, I try.

# How can I build the source?

Use [Eclipse](https://www.eclipse.org/downloads/) with [IvyDE](https://marketplace.eclipse.org/content/apache-ivyde%E2%84%A2)

