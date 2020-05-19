Angular Socialshare
==================

![Angular socialshare](http://i.imgur.com/dvoeyBu.png)

[![Join the chat at https://gitter.im/720kb/angular-socialshare](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/720kb/angular-socialshare?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


Angular Socialshare is an angularjs directive for sharing urls and content on social networks such as (facebook, google+, twitter, pinterest and so on).


The Angular Socialshare is developed by [720kb](http://720kb.net).

#### Requirements


AngularJS v1.3+

#### Browser support


Chrome | Firefox | IE | Opera | Safari
--- | --- | --- | --- | --- |
 ✔ | ✔ | IE9 + | ✔ | ✔ |


## Load

To use the directive, include the angular socialshare's javascript file in your web page:

```html
<!DOCTYPE HTML>
<html>
<body ng-app="app">
  //.....
  <script src="src/js/angular-socialshare.js"></script>
</body>
</html>
```

## Installation


#### Bower

```bash
$ bower install angularjs-socialshare --save
```
#### npm

```bash
$ npm install angular-socialshare --save
```

_then [load](https://github.com/720kb/angular-socialshare#load) it in your html_

#### Add module dependency
Add the 720kb.socialshare module dependency

```javascript
angular.module('app', [
  '720kb.socialshare'
 ]);
```

Call the directive wherever you want in your html page

```html
<a href="#"
socialshare
socialshare-provider="twitter"
socialshare-text="720kb AngularJS Socialshare"
socialshare-hashtags="angularjs, angular-socialshare"
socialshare-url="http://720kb.net">
Share me
</a>
```
OR

Call the Socialshare [service](#service)

```javascript
  .controller('Ctrl', ['Socialshare', function testController(Socialshare) {

    Socialshare.share({
      'provider': 'facebook',
      'attrs': {
        'socialshareUrl': 'http://720kb.net'
      }
    });
```


## Usage

Angular socialshare allows you to use sharing options via `attribute` data

#### Sharing Provider

You can set the social platform you want to share on using the `socialshare-provider=""` attribute.

##### Providers:

- [email](#email)
- [facebook](#facebook)
- [facebook-messenger](#facebook-messenger)
- [twitter](#twitter)
- [linkedin](#linkedin)
- [google](#google-plus)
- [pinterest](#pinterest)
- [tumblr](#tumblr)
- [reddit](#reddit)
- [stumbleupon](#stumbleupon)
- [buffer](#buffer)
- [digg](#digg)
- [delicious](#delicious)
- [vk](#vk)
- [ok](#vk)
- [pocket](#pocket)
- [wordpress](#wordpress)
- [flipboard](#flipboard)
- [xing](#xing)
- [hackernews](#hacker-news)
- [evernote](#evernote)
- [whatsapp](#whatsapp)
- [telegram](#telegram)
- [viber](#viber)
- [skype](#skype)
- [sms](#sms)
- [weibo](#weibo)

Please use them all in lowercase (`socialshare-provider="delicious"`)

## Doc

#### Facebook

(`socialshare-provider="facebook"`)

> As of April 2017 - If you want to share a photo and customize the previews you must use [Open Graph Metas](https://developers.facebook.com/docs/sharing/webmasters#markup)

`simple sharer` = [Facebook simple share](https://developers.facebook.com/docs/plugins/share-button) , `share` = [Facebook Dialog Share](https://developers.facebook.com/docs/sharing/reference/share-dialog),    `feed` = [Facebook Dialog Feed](https://developers.facebook.com/docs/sharing/reference/feed-dialog), `send` = [Facebook Dialog Send](https://developers.facebook.com/docs/sharing/reference/send-dialog)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
share, feed, send, **simple sharer**|socialshare-url=""|page URL|false|Set the url/link to share
feed, send, share|socialshare-type=""|String('feed' or 'send' or 'share')|**simple sharer**|Use a **simple sharer** or Dialog Send or Dialog Share or Dialog Feed
feed, send, share|socialshare-via=""|String|false|Set the FB APP ID value
feed, send|socialshare-to=""|String|false|Set the to value
feed | socialshare-from=""|String|false|Set the from to value
feed, send|socialshare-ref=""|String('comma,separated')|false|Set the ref value
feed, send, share|socialshare-display=""|String('popup')|false|Set the display value
share|socialshare-quote=""|String|false|Set the display text
share|socialshare-hashtags=""|String|false|Set the display value along with # Eg:#facebook (one only hashtag)
feed|socialshare-source=""|URL|false| Set the URL of a media file (either SWF or MP3) attached to this post
feed, send|socialshare-redirect-uri=""|URL|false|Set the redirect URI
share|socialshare-mobileiframe=""|boolean|false|If set to true the share button will open the share dialog in an iframe on top of your website. This option is only available for mobile, not desktop.

#### Facebook Messenger
`mobile only` - (works only for `<a>` elements, it is a direct link)

(`socialshare-provider="facebook-messenger"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share


#### Twitter

(`socialshare-provider="twitter"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL | page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-via=""|String('username')|false|Set the via to share
sharer|socialshare-hashtags=""|String('hash,tag,hastag')|false|Set the hashtags to share


#### Linkedin

(`socialshare-provider="linkedin"`)

Method | Option | Type | Default | Description
-------------|-------------|-------------|-------------|-------------
sharer|socialshare-url=""|URL|pageURL|Set the url to share
sharer|socialshare-text=""|String|false|Set the title value that you wish to use
sharer|socialshare-description=""|String|false|Set the description value that you wish to use
sharer|socialshare-source=""|String|false|Set the source of the content

#### Reddit

(`socialshare-provider="reddit"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|pageURL|Set the url to share
sharer|socialshare-text=""|String|false|Set the text content to share
sharer|socialshare-subreddit=""|String('technology')|false|Set the subreddit to share on

#### Vk

(`socialshare-provider="vk"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the title to share
sharer|socialshare-description=""|String|false| Set the content to share
sharer|socialshare-media=""|URL|false|Set the image source to share
 
#### OK

(ok.ru)

(`socialshare-provider="ok"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share

#### Digg

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-media=""|URL|false|Set the media url to share

#### Delicious

(`socialshare-provider="delicious"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-media=""|URL|false|Set the media url to share

#### StumbleUpon

(`socialshare-provider="stumbleupon"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-media=""|URL|false|Set the media url to share

#### Pinterest

(`socialshare-provider="pinterest"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-media=""|URL|false|Set the media url to share

#### Google (Plus)

(`socialshare-provider="google"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share

#### Tumblr

(`socialshare-provider="tumblr"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-media=""|URL|false|Set the media url to share

#### Buffer

(`socialshare-provider="buffer"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-media=""|URL|false|Set the media url to share
sharer|socialshare-via=""|URL|false|Set the buffer via

#### Pocket

(`socialshare-provider="pocket"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share

#### Flipboard

(`socialshare-provider="flipboard"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share

#### Evernote

(`socialshare-provider="evernote"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share


#### Hacker News

(`socialshare-provider="hackernews"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share

#### Wordpress

(`socialshare-provider="wordpress"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-media=""|URL|false|Set the media url to share


#### Xing

(`socialshare-provider="xing"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-media=""|URL|false|Set the media url to share
sharer|socialshare-follow=""|URL|false|	Set the Xing page url which will be then suggested to you to follow


#### Whatsapp
`mobile only` - (works only for `<a>` elements, it is a direct link)

(`socialshare-provider="whatsapp"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share

#### Telegram

(`socialshare-provider="telegram"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share


#### Viber
`mobile only` -  (works only for `<a>` elements, it is a direct link)

(`socialshare-provider="viber"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share


#### Skype

(`socialshare-provider="skype"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share

#### Email

(`socialshare-provider="email"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
mailto|socialshare-subject=""|String|false|Set the subject for the email
mailto|socialshare-body=""|String|false|Set the body content for the email
mailto|socialshare-to=""|String|false|Set the Receiver / Receivers
mailto|socialshare-cc=""|String|false|Set the CC / CCs for the email
mailto|socialshare-bcc=""|String|false|Set the BCC / BCCs for the email

#### Sms
(works only for `<a>` elements, it is a direct link)

(`socialshare-provider="sms"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share
sharer|socialshare-to=""|URL|false|Set the phone number of the contact


#### Weibo

(`socialshare-provider="weibo"`)

Method | Option | Type | Default | Description
------------- | ------------- | ------------- | ------------- | -------------
sharer|socialshare-url=""|URL|page URL|Set the url to share
sharer|socialshare-text=""|String|false|Set the content to share

## Options

#### Sharing Popup Size
You can set a specific Height or Width for the sharing popup using the `socialshare-popup-height=""` and `socialshare-popup-width=""` attributes (sometimes, if if the popup is too small, it gets resized by third parties)

```html
<a href="#"
socialshare
socialshare-provider="reddit"
socialshare-url="http://720kb.net"
socialshare-text="Sharing it!"
socialshare-popup-height="800"
socialshare-popup-width="800">
Share with a bigger popup
</a>
```

#### Sharing Event Trigger
You can choose to bind a different event trigger for showing up the sharer popup using the `socialshare-trigger=""` attribute (you can use any angular `element.bind()` event you want)

```html
<a href="#"
socialshare
socialshare-provider="reddit"
socialshare-text="Sharing on mouseover"
socialshare-trigger="mouseover">
Share me when mouse is over
</a>
```
or a set of

```html
<a href="#"
socialshare
socialshare-provider="reddit"
socialshare-text="Sharing on mouseover"
socialshare-trigger="focusout mouseleave">
Share me when focusout or mouseleave
</a>
```

## Service
You may need to share from a controller (for example), this is how to use the `Socialshare` service:

```javascript
  .controller('Ctrl', ['Socialshare', function testController(Socialshare) {

    Socialshare.share({
      'provider': 'facebook',
      'attrs': {
        'socialshareUrl': 'http://720kb.net'
      }
    });

    Socialshare.share({
      'provider': 'twitter',
      'attrs': {
        'socialshareUrl': 'http://720kb.net',
        'socialshareHashtags': '720kb, angular, socialshare'
      }
    });
    //every attrs must be in camel case as showed above
    //this will open the share popup immediately without any trigger event required
```
_Some providers (specially mobile provider, such as: Viber, Whatsapp etc..) do not work with a Service call, because their API or Usage does not allow a trigger event on them_

## Globals

#### Provider setup
Sometimes you may need to set default values for all the sharing buttons, here is how to setup this:

```javascript
.config(['socialshareConfProvider', function configApp(socialshareConfProvider) {

  socialshareConfProvider.configure([
    {
      'provider': 'twitter',
      'conf': {
        'url': 'http://720kb.net',
        'text': '720kb is enough',
        'via': 'npm',
        'hashtags': 'angularjs,socialshare,angular-socialshare',
        'trigger': 'click',
        'popupHeight': 800,
        'popupWidth' : 400
      }
    },
    {
      'provider': 'facebook',
      'conf': {
        'url': 'http://720kb.net',
        'trigger': 'mouseover',
        'popupHeight': 1300,
        'popupWidth' : 1000
      }
    }
  //and so on ...
  ]);
}]);
```
*NB* if you define the provider settings, but then you change the option value by html attributes, the html attribute value will be the final one (the one that will be used)


#### [Live demo](https://720kb.github.io/angular-socialshare)


## Contributing

We will be much grateful if you help us making this project to grow up.
Feel free to contribute by forking, opening issues, pull requests etc.

## License

The MIT License (MIT)

Copyright (c) 2014 Filippo Oretti, Dario Andrei

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
