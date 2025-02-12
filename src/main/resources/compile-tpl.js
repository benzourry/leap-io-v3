const hashCode = function (s) {
    let h = 0, i = s.length;
    while (i > 0) {
        h = (h << 5) - h + s.charCodeAt(--i) | 0;
    }
    return h;
};

const tplCache = {};

export function multiReplace(text, correction) {
    const reg = new RegExp(Object.keys(correction).join("|"), "g");
    return text.replace(reg, (matched) => correction[matched]);
}
const tag2sym = { table: 'table_', tr: 'tr_', td: 'td_', th: 'th_', tbody: 'tbody_', thead: 'thead_', src:'src_' }
const sym2tag = { table_: 'table', tr_: 'tr', td_: 'td', th_: 'th', tbody_: 'tbody', thead_: 'thead', src_:'src' }

export function compileTpl(templateText, data) {
    let code;
    let tplHashCode = hashCode(templateText);
    // let templateText = rawTpl;
    if (tplCache[tplHashCode]) {
        code = tplCache[tplHashCode];
    } else {
        /// NEW Support for x-foreach and x-if as attributes
        /// Problem with current <x-foreach> with table because of parseFromString
        try {
            // use createElement allow styles
            // var doc: Document = new DOMParser().parseFromString(templateText, 'text/html');
            // problem mun dlm x-if da table fulltpl (xda tbody) fail utk replace oritag (ada tbody sbb querySelector) so, maintain code lamak n xpa
            // var fulltpl = templateText;
            // replace sensitive tag such as table, etc to prevent doc refactoring
            let fulltpl = multiReplace(templateText, tag2sym);
            // let fulltpl = templateText.replace(/table|tr|td|th|tbody|thead/gi, m=>replacement[m])

            let doc = document.createElement("x-template");
            // need root element to use querySelector, use noscript to prevent image being loaded
            // revert back to x-template because noscript issue with inline x-foreach, x-for and x-if
            doc.innerHTML = fulltpl;

            let fn = function (doc, xscpl) {
                let elems = doc.querySelectorAll([${xscpl}]);
                elems.forEach(e => {
                    // console.log(e)
                    let oritag = e.outerHTML; // original tag with attributes
                    let scpl = e.getAttribute(xscpl);
                    e.removeAttribute(xscpl);
                    fulltpl = fulltpl.replace(oritag, `<!--##--${xscpl} $="${scpl}"!--##-->${e.outerHTML}<!--##--/${xscpl}!--##-->`);
                })
            }
            fn(doc, 'x-foreach');
            fn(doc, 'x-for');
            fn(doc, 'x-if');

            templateText = multiReplace(fulltpl.replace(/!--##--/gi, ""), sym2tag);

            // console.log(templateText);
            // templateText = fulltpl.replace(/!--##--/gi, "").replace(/table_|tr_|td_|th_|tbody_|thead_/gi, m=>replaceBack[m]);

        } catch (err) {
            console.log(err);
        }
        //// END

        code = ("Object.assign(this,data);var output=" +
            (JSON.stringify(templateText) || templateText)
                .replace(/\<\!\-\-(.+?)\-\-\>/g, '') // remove <!-- -->
                .replace(/\{\{(.+?)\}\}/g, r$val) // replace {{}}
                .replace(/\[\#(.+?)\#\]/g, r$script) // replace [##]
                .replace(/<x-if\s*\$=\\\"(.+?)\\\"\s*>/ig, '";if($1){\noutput+="') // replace <x-if x="true">
                .replace(/<x-else\s*\/?\s*>/ig, '";}else{\noutput+="')
                .replace(/<x-else-if\s*\$=\\\"(.+?)\\\"\s*\/?\s*>/ig, '";}else if($1){\noutput+="')
                .replace(/<\/x-if>/ig, '";}\noutput+="')
                .replace(/<x-for\s*\$\=\\\"(.+?)\\\"\s*>/ig, '";for($1){\noutput+="') // replace <x-for x="i=0;i<5;i++">
                .replace(/<\/x-for>/ig, '";}\noutput+="')
                .replace(/<x-foreach\s*\$=\\\"(.+?)\\\"\s*>/ig, r$foreach) // replace <x-foreach x="i of list">
                .replace(/<\/x-foreach>/ig, '";})\noutput+="')
                .replace(/\<\?(.+?)\?\>/g, '";$1\noutput+="') + //replace <??>
            ";return output;").replace(/(?:\\[rnt])+/gm, ""); // remove newline\tab\return
        tplCache[tplHashCode] = code;
    }

    // console.log(code)

    if (templateText && data) {
        data.dayjs = dayjs;
        let result = "";

        try {
            result = new Function(
                "data", "get", code)(data, get);
        } catch (err) {
            throw err;
        }
        return result;
    } else {
        return templateText;
    }
}

function r$foreach(match, p1) {
    // x-foreach="let a of list|filter:a.name=='sad'"
    // let inside = match.replace(/<x-foreach x=\\\"/ig, '').replace(/\\\">/ig,'');
    let part = p1.split(" of ");
    return ";${part[1].trim()} && ${part[1].trim()}?.forEach(function(${part[0].trim()},$index){\noutput+=";
}
function r$script(match, p1) {
    // match.replace(/\[#|#\]/ig,'').
    return ";${p1.replace(/&nbsp;/ig, '')}\noutput+="
}

function r$val(match, p1, p2, p3, offset, string) {
    let aVal = "";
    // console.log("m:###"+match);
    // let inside = match.replace(/([{}]+)/ig, '')
    p1 = p1.replace(/\\"/g, '"');
    let regex = /(['"].?["']|[^"|:\s]+)(?=\s|:\s*$)/ig;
    let splitted = p1.match(regex);
    // "+get(function(){return      },"")+"
    if (splitted.length > 1) {
        switch (splitted[1]) {
            case 'date':
                aVal = key('(new Date(' + splitted[0] + ')).toDateString()');//.toUTCString();
                if (splitted[2]) {
                    aVal = key(dayjs(new Date(${splitted[0]})).format(${splitted[2]}));
                }
                break;
            case 'imgSrc':
            case 'src':
                let type = "";
                if (splitted[2]) {
                    type = splitted[2] + "/";
                }
                aVal = baseApi + '/entry/file/' + type + key(splitted[0], 'encodeURI');
                break;
            case 'qr':
                aVal = baseApi + '/form/qr?code=' + key(splitted[0]);
                break;
            case 'json':
                aVal = key(`JSON.stringify(${splitted[0]})`);
                break;
            default:
                aVal = key(p1);
        }
    } else {
        aVal = key(p1);
    }
    return aVal;
}

function key(key, wrapFn) {
    return '"+get(function(){return ' + key + ' },"",' + wrapFn + ')+"';
}

function get(fn, defaultVal, wrapFn) {
    try {
        var val = wrapFn ? wrapFn(fn()) : fn();
        return (typeof val == "undefined" || val === null) ? defaultVal : val;
    } catch (e) {
        return defaultVal;
    }
}