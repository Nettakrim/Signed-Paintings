# In-game uploading has been removed in the latest version (1.2.0)

## Why?

Imgur has repeatedly shown itself to be unreliable.

- It changed how it provided direct links to images, redirecting to the html page of the image. This broke every version of the mod simultaneously
  - luckily the only change needed was to the http headers, so backwards compatibility was maintained
  - see [my rablings at the time](https://modrinth.com/mod/signed-paintings/version/1.0.5)
- It barely worked anyway because it was always rate-limited
  - I start to suggest that people just upload images to discord instead, and tell them how to do the manual allowing of domains (and eventually add it as a default).
    - discord would later start expiring the links to uploaded files (objectively because of people like me using it as an image hoster but thats besides the point)
  - I did some short term fixes by switching up the api key in different versions of the mod, and letting them fallback to eachother, but theyre all destined to get rate limited anyway
  - I decide that its not actually *that* obtuse to have the mod require users to get their own api keys to use the uploading feature, so started work on it
  - In the middle of working on it, imgur stopped giving people api keys, redirecting https://api.imgur.com/oauth2/addclient to https://imgur.com/
  - Dispirited, i partially implement domain allowing as an alternative to uploading, but give up for some time
- Most recently, imgur blocks the UK ([BBC Article](https://www.bbc.co.uk/news/articles/c4gzxv5gy3qo), [Reddit Summary](https://www.reddit.com/r/RimWorld/comments/1nufmze/imgur_now_blocked_in_the_uk_steam_workshop_pages/))
  - Seeing this, i fully give up on imgur, and make the half-implemented domain allowing from earlier into the main way the mod works

## So how does it work now?

Now, when pasting a link into a sign, the button that used to say "Upload to Imgur" says "Trust Domain" - likewise, when the client loads a sign with an image link from an untrusted domain, it will prompt them in chat to trust it

Imgur is trusted by default, this means if you go into a world with existing signed paintings, they will still work! (unless youre in the UK, of course)

The only situation older images wont work is if they are from a 1.0.X version, since now only compressed data (the mess of i|'i|'|| that signed paintings have) will be loaded as an image

The reason i required images to be imgur to begin with was because having the client load random urls can have security issues. letting the player trust domains does open them up to this, but not majorly

## Why dont you just use-

While i did complain about issues with Imgur and Discord specifically, there is no reason your favourite image hoster would be immune to the same issues. (especially the last one! internet censorship does suck but, for now, it is the world we live in,, and image hosters are particularly susceptible to it)

A lot of the alternatives ive looked at are weirdly lacking in clear descriptions of api limitations etc, and they are all limited in some way - i dont want to end up with the mod constantly rate limited (again!), and i also dont know why an image upload service that *isnt* rate limited wouldnt say so in their advertising (i am literally their target audience, why dont they tell me what i want to know!!)

People have also, reasonably, complained that they cant trust imgur to keep their images safe etc - your favourite image hoster is almost certainly untrustworthy to someone else. I picked Imgur in part because it is the most recognisable to the average user!

Most importantly, i do not want a ***single point of failure*** - if a hoster gets discontinued, deletes your images based on its own changable criteria, *stops operating in the UK*, etc, then it would take the whole mod with it, and i, or a kind contributor, would have to re-do a bunch of the mod for a new image hoster, with its new set of problems

Some users have also wanted to use their own image servers, by making domain trusting the default, instead of buried in the config file, this is now much easier for them! in fact, that leads on perfectly to my next point:

## Future Plans

The *only* image hoster i can trust to be uniquely able to survive for as long as the mod exists is the mod itself

At some point in the unspecified future, i want to add a button that will store the image, as a nice and dependable file, in the singleplayer world folder / global config folder / server folder

However, this will be *secondary* to just trusting the domain - the first step is what ive done for version 1.2.0, which is tear out all of the image uploading code