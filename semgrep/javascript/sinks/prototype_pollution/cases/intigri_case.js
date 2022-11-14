const express = require("express");
const bodyParser = require('body-parser');
const { inviteCode } = require('./secret');
const app = express();
app.use(bodyParser.text());

const port = 3000
const baseUser = {'pic':"d.png"}

app.post('/', (req, res) =>{
    let user = JSON.parse(req.body);
    if (user.isAdmin && user.inviteCode !== inviteCode) {
        res.send('No invite code? No admin!');
    } else {
        let newUser = Object.assign(baseUser, user);
        if (newUser.isAdmin) createAdmin(newUser);
        else createUser(newUser);
        res.send('Successfully created' + `${newUser.isAdmin ? 'Admin' : 'User'}`);
    }
})

app.listen(port, () => {
    console.log(`App listening on port ${port}`);
})

// Ans: https://gist.github.com/Sudistark/95b6240b1692b2987ed0bd2183350b8d