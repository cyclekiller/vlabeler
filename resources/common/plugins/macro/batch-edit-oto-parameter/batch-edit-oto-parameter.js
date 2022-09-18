let selectedEntryIndexes = params["selector"]
let parameterName = params["parameter"]
let hasLeft = labeler.fields.length > 3 // true if labeler is oto-plus with a standalone "left" field
let keepDistance = params["keepDistance"]

let nameTexts = [
    ["offset", ["Offset", "左边界"]],
    ["fixed", ["Fixed", "固定"]],
    ["overlap", ["Overlap", "重叠"]],
    ["preutterance", ["Preutterance", "先行发声"]],
    ["cutoff", ["Cutoff", "右边界"]]
]

let expression = params["expression"]
for (let param of nameTexts) {
    let key = param[0]
    let texts = param[1]
    for (let text of texts) {
        expression = expression.replace(`\${${text}}`, key)
    }
}

let unknownExpressionMatch = expression.match(/\$\{\w+}/)
if (unknownExpressionMatch) {
    error({
        en: `Unknown parameter in input expression: ${unknownExpressionMatch[0]}`,
        zh: `输入的表达式中包含未知参数：${unknownExpressionMatch[0]}`
    })
}

if (debug) {
    console.log(`Input entries: ${entries.length}`)
    console.log(`Selected entries: ${selectedEntryIndexes.length}`)
    console.log(`Expression: ${expression}`)
    console.log(`Parameter: ${parameterName}`)
}

output = entries.map((entry, index) => {
    if (!selectedEntryIndexes.includes(index)) {
        return new EditedEntry(index, entry)
    }
    let edited = Object.assign({}, entry)
    let offset = entry.start
    if (hasLeft) {
        offset = entry.points[3]
    }
    let fixed = entry.points[0] - offset
    let preutterance = entry.points[1] - offset
    let overlap = entry.points[2] - offset
    let cutoff = entry.end
    if (cutoff > 0) {
        cutoff = -(cutoff - offset)
    } else {
        cutoff = -cutoff
    }
    let newValue = null
    try {
        newValue = eval(expression)
    } catch (e) {
        throwExpectedError({
            en: "Falied to calculate the new value, cause: " + e.message,
            zh: "计算新值失败，原因：" + e.message
        })
    }

    function moveAll(entry, diff, isSetEnd = false) {
        entry.start += diff
        if (!isSetEnd && entry.end <= 0) {
            // if end is moved by diff (not set directly) and is negative, it should not be over 0
            entry.end += diff
            if (entry.end > 0) {
                entry.end = 0
            }
        } else {
            entry.end += diff
        }
        entry.points = entry.points.map(p => p + diff)
    }

    let diff = 0
    if (nameTexts.find(x => x[0] === "offset")[1].includes(parameterName)) {
        if (hasLeft) {
            if (keepDistance) {
                diff = newValue - entry.points[3]
                moveAll(edited, diff)
            } else {
                edited.points[3] = newValue
            }
        } else {
            if (keepDistance) {
                diff = newValue - entry.start
                moveAll(edited, diff)
            } else {
                edited.start = newValue
            }
        }
    } else if (nameTexts.find(x => x[0] === "fixed")[1].includes(parameterName)) {
        if (keepDistance) {
            diff = newValue + offset - entry.points[0]
            moveAll(edited, diff)
        } else {
            edited.points[0] = newValue + offset
        }
    } else if (nameTexts.find(x => x[0] === "preutterance")[1].includes(parameterName)) {
        if (keepDistance) {
            diff = newValue + offset - entry.points[1]
            moveAll(edited, diff)
        } else {
            edited.points[1] = newValue + offset
        }
    } else if (nameTexts.find(x => x[0] === "overlap")[1].includes(parameterName)) {
        if (keepDistance) {
            diff = newValue + offset - entry.points[2]
            moveAll(edited, diff)
        } else {
            edited.points[2] = newValue + offset
        }
    } else if (nameTexts.find(x => x[0] === "cutoff")[1].includes(parameterName)) {
        if (newValue < 0) {
            if (keepDistance) {
                diff = -newValue + offset - entry.end
                moveAll(edited, diff, true)
            } else {
                edited.end = -newValue + offset
            }
        } else {
            if (keepDistance) {
                diff = -newValue - entry.end
                moveAll(edited, diff, true)
            } else {
                edited.end = -newValue
            }
        }
    }

    if (hasLeft) {
        edited.start = Math.min(...edited.points, edited.start)
    }

    if (debug) {
        console.log(`Edited: ${JSON.stringify(edited)}`)
    }
    return new EditedEntry(index, edited)
})
