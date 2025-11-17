var OpenElis = OpenElis || {}
OpenElis.Utils = {
    calculateAge: function(DOB, datePattern) {
        var date = new String(DOB);
        var splitPattern = datePattern.split("/");
        var dayIndex = 0;
        var monthIndex = 1;
        var yearIndex = 2;

        for (var i = 0; i < 3; i++) {
            if (splitPattern[i] == "DD") {
                dayIndex = i;
            } else if (splitPattern[i] == "MM") {
                monthIndex = i;
            } else if (splitPattern[i] == "YYYY") {
                yearIndex = i;
            }
        }

        var splitDOB = date.split("/");
        var monthDOB = splitDOB[monthIndex];
        var dayDOB = splitDOB[dayIndex];
        var yearDOB = splitDOB[yearIndex];

        var today = new Date();

        var adjustment = 0;

        if (!monthDOB.match(/^\d+$/)) {
            monthDOB = "01";
        }

        if (!dayDOB.match(/^\d+$/)) {
            dayDOB = "01";
        }

        //months start at 0, January is month 0
        var monthToday = today.getMonth() + 1;

        if (monthToday < monthDOB ||
            (monthToday == monthDOB && today.getDate() < dayDOB  )) {
            adjustment = -1;
        }

        return today.getFullYear() - yearDOB + adjustment;
    },

    getXMLValue: function(response, key){
        var field = response.getElementsByTagName(key).item(0);
        return field != null ? field.firstChild.nodeValue : "";
    },

    getTime: function (time) {
        // Ensure we have a string in HH:mm format
        if (!time) return "";
        
        // Parse hours and minutes
        var parts = time.toString().split(':');
        if (parts.length !== 2) return time; // Return original if not in HH:mm format
        
        var hours = parseInt(parts[0], 10);
        var minutes = parts[1];
        
        // Determine AM/PM
        var meridiem = hours >= 12 ? 'PM' : 'AM';
        
        // Convert to 12-hour format
        hours = hours % 12;
        hours = hours ? hours : 12; // Convert 0 to 12
        
        // Format with padding and AM/PM
        return hours + ':' + minutes + ' ' + meridiem;
    }

}
