/** * CodaBubble extension for jQuery library * * Creates tooltips "coda bubble" style * * @author Carlo Tasca * @version 1.0 * * OPTIONS: * * @param {array} distances Distances of bubbles from their triggers  * @param {array} leftShifts Left positions of bubbles * @param {array} bubbleTimes Life times for bubbles * @param {array} hideDelays Hide delay times for bubbles * @param {array} bubbleWidths Hide delay times for bubbles * @param {string} bubbleImagesPath Path to skin for bubbles * @param {boolean} msieFix Fix for IE png rendering. Replaces pngs with gifs if true (default) * @param {boolean} msiePop If false removes bubble in IE */
jQuery.fn.codaBubble = function(opts) {
    var bubble = this;
    opts = jQuery.extend({            distances : [20],            leftShifts : [30],              bubbleTimes : [400],              hideDelays : [0],              bubbleWidths : [200],              bubbleImagesPath : "",              msieFix : true,              msiePop : true        }, opts || {});
    function bubbleHtmlWrapper(bubbleHtml) {
        return '<table class="popup" style="opacity: 0; top: -120px; left: -33px; display: none;"><tr><td class="corner topleft"/><td class="top"/><td class="corner topright"/></tr><tr><td class="left"/><td class="bubble_content">' + bubbleHtml + '</td><td class="right"/></tr><tr><td class="corner bottomleft"/><td class="bottom"><img style="display:block;" width="30" height="29" alt="" /></td><td class="corner bottomright"/></tr></table>';
    }

    return jQuery(bubble).each(function (i) {
        var bubbleHtml = jQuery('.bubble_html', this).html();
        jQuery('.bubble_html', this).hide().after(bubbleHtmlWrapper(bubbleHtml));
        jQuery('.popup td.bottom img', this).attr('src', opts.bubbleImagesPath + '/bubble-tail2.png');
        if (opts.msieFix) {
            if ($.browser.msie) {
                jQuery('.popup td.topleft').css('background-image', 'url(' + opts.bubbleImagesPath + '/bubble-1.gif)');
                jQuery('.popup td.top').css('background-image', 'url(' + opts.bubbleImagesPath + '/bubble-2.gif)');
                jQuery('.popup td.topright').css('background-image', 'url(' + opts.bubbleImagesPath + '/bubble-3.gif)');
                jQuery('.popup td.left').css('background-image', 'url(' + opts.bubbleImagesPath + '/bubble-4.gif)');
                jQuery('.popup td.right').css('background-image', 'url(' + opts.bubbleImagesPath + '/bubble-5.gif)');
                jQuery('.popup td.bottomleft').css('background-image', 'url(' + opts.bubbleImagesPath + '/bubble-6.gif)');
                jQuery('.popup td.bottom').css('background-image', 'url(' + opts.bubbleImagesPath + '/bubble-7.gif)');
                jQuery('.popup td.bottomright').css('background-image', 'url(' + opts.bubbleImagesPath + '/bubble-8.gif)');
                jQuery('.popup td.bottom img').attr('src', opts.bubbleImagesPath + '/bubble-tail2.gif');
            }
        }
        var distance = opts.distances[i];
        var time = opts.bubbleTimes[i];
        var hideDelay = opts.hideDelays[i];
        var hideDelayTimer = null;
        var beingShown = false;
        var shown = false;
        var trigger = jQuery('.trigger', this);
        var popup = jQuery('.popup', this).css('opacity', 0);
        jQuery([trigger.get(0), popup.get(0)]).mouseover(function () {
            jQuery(popup).css("width", opts.bubbleWidths[i] + "px");
            var triggerWidth = jQuery(trigger.get(0)).css('width');
            if (hideDelayTimer) clearTimeout(hideDelayTimer);
            if (beingShown || shown) {
                return;
            } else {
                beingShown = true;
                popup.css({              top: -100,              left: opts.leftShifts[i],              display: 'block'            }).animate({              top: '-=' + distance + 'px',              opacity: 1            }, time, 'swing', function() {
                    beingShown = false;
                    shown = true;
                });
            }
        }).mouseout(function () {
            if (hideDelayTimer) clearTimeout(hideDelayTimer);
            hideDelayTimer = setTimeout(function () {
                hideDelayTimer = null;
                popup.animate({              top: '-=' + distance + 'px',              opacity: 0            }, time, 'swing', function () {
                    shown = false;
                    popup.css('display', 'none');
                });
            }, hideDelay);
        });
        if (!opts.msiePop && $.browser.msie) {
            jQuery(popup).remove();
        }
    });
}
