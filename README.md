# Android Application about "Online Ordering for Clothing Shop With Sales And Inventory Report"

This application show cases my journey as an android developer.

## Tech Stack:
- Navigation Architecture
- Retrofit
- GSON
- Paging 3
- Glide
- Dagger Hilt
- Room Database
- Kotlin Coroutines and Flow
- Firebase Cloud Messaging for push notifications.

**Remote database used:** Firebase Firestore

**Online Payment SDK used:** Paypal SDK
## Features

- This application have two (2) user sides which are Customer and Admin
    - ### Admin
        1. **Categories Module** - This module allows the admin/s to create, update, and delete categories. Deleting a category will also delete all the products under that category.
        2. **Products Module** - Admin/s can create, update and delete products in this module. This module includes all information about a certain product like inventory, product images and reviews.
        3. **Product Detail** - This allows admin/s to view the product in a more organized manner.
        4. **Orders Module** - This module displays all orders, including all products and information about the customer who placed the order. The completed transactions are presented at the bottom of this module, and the orders can be sorted from old to new. Admin/s can view all the shipped, shipping, for delivery, completed, cancelled, and returned orders/items.
        5. **Order Details Module** - In this module, admin/s can see all information about an order: the delivery information (that was provided by the customer), list of product quantity (if there is more than one ordered product), product size and the subtotal.
        6. **News Feed Module** - This module allows the admin/s to create and delete announcements. These announcements are going to appear once a customer opens the application and chooses to go to this module. The purpose of this module is to notify customers when there are new products and to keep them updated. All admins can add new post but cannot delete post that they do not own. Each post has a title, description, image, and the date it was posted. Both the customers and admin/s can see how many people liked a post.
        7. **Accounts Module** - This module allows the admin/s to ban and unban an account, if the administrator suspects a harmful /suspicious action using that account.
        8. **Inventory Report Module** - This module displays the current inventory levels for all products in the application, such as the number of items committed, sold, and returned. This also allows admin/s to add a new size to a product including its stock count.
        9. **Sales Report Module** - This module allows the admin/s to view the sales for the current month and year as well as the overall sales for the previous days/s, month/s, and year/s. For easy retrieval, the sales can also be filtered by day, month, and year.
        10. **Audit Trail / History Log Module** - This module allows the admin/s to view the activities done by all the admins. The admin can monitor the modifications made to the application, as well as who made them and when they were made.
        11. **Fast Moving Report** - This module allows the admin/s to see the most product sold in the date range.
        12. **Slow Moving Report** - This module allows the admin/s to see the least product sold in the date range.
    - ### Customer
        1. **Home Module** - Before this module appears, the Midnightmares logo is going to appear on the splash screen first. Customers may now access categories, news feed, carts, and their profiles using this module.
        2. **Registration Module** - Customers may use their Gmail or email in signing in. If a customer is a first-time user of the application, an email verification will be sent to ensure that the email address is valid and active. The app will retrieve the customer's display name and profile image from their provider.
        3. **Login Module** - This module allows the customer to log in using Google or Email. This is done to ensure that the customer is the real owner of that Gmail or Email before they can access the app.
        4. **News Feed Module** - This module allows the customers to see all the announcements and the release of the new product published by the administrator. They can also like the administrators' posts.
        5. **Profile Module** - This module allows the customer to edit their first name, last name, birthdate, and manage their addresses.
        6. **Categories Module** - The module allows the customer to view all of Midnightmares' categories. It will be visible on the home page, which even guests can see. Aside from the category name, each category has a picture so that customers may get a better understanding of the category.
        7. **Product Module** - This module displays all products based on the category to which they belong. Customers may view the product's name, image, and price.
        8. **Product Detail Module** - A customer can access this module once they click on a certain product. It includes information on the product, such as images, a description, the current rating, and customer feedback. They may also add the product they want to buy to their cart or Wishlist. There is also a button that allows customers to share the product by providing a Link.
        9. **Cart Module** - This module displays all the items that a customer has added to his/her cart. Customers can delete products they do not want or need, alter their quantity/variation, and see the total price in real time.
        10. **Orders Module** - This module displays a customer's purchasing history. The categories will be arranged as follows: "Shipping", "Shipped", "Delivery", "Completed” and “Returned”.
        11. **Cancel Order Module** - This module allows customers to cancel their orders if they wish to within 24 hours after placing the order.
        12. **Replace / Exchange Product Module** - If there are any defects or faults, this module allows the customer to swap their prior order for a similar product at the same price. If a customer wants a product that is more expensive than the previous order, the customer must pay the difference.
        13. **Checkout Module** - This module asks for customer information such as name, phone number, and shipping address before placing an order. They must also select a payment option that is suitable for them. If they are in Metro Manila or NCR Plus, they can choose Cash-on-delivery (COD). If they are outside of Metro Manila or NCR Plus, they can choose credit/debit card.
        14. **Frequently Asked Questions (FAQs) Module** - This module presents a list of commonly asked questions whether it is about the business or the application itself. The answers will be provided by the business owner and the developers.
        15. **Terms & Condition Module** - This module contains the terms, conditions, and policies that customers must accept before using or accessing the Midnightmare Application.
        16. **Size Chart Module** - This module presents the measurement of shirts and shorts according to the size chart of the business owner.

## Screenshots

### Customer Side:

<p>
<img src="/media/client_01.png" width="32%"/>
<img src="/media/client_02.png" width="32%"/>
<img src="/media/client_03.png" width="32%"/>
</p>
<p>
<img src="/media/client_04.png" width="32%"/>
<img src="/media/client_05.png" width="32%"/>
<img src="/media/client_06.png" width="32%"/>
</p>
<p>
<img src="/media/client_07.png" width="32%"/>
<img src="/media/client_08.png" width="32%"/>
<img src="/media/client_09.png" width="32%"/>
</p>
<p>
<img src="/media/client_10.png" width="32%"/>
<img src="/media/client_11.png" width="32%"/>
<img src="/media/client_12.png" width="32%"/>
</p>
<p>
<img src="/media/client_13.png" width="25%"/>
<img src="/media/client_14.png" width="25%"/>
<img src="/media/client_15.png" width="25%"/>
<img src="/media/client_16.png" width="25%"/>
</p>
<p>

### Admin Side:

<p>
<img src="/media/admin_01.png" width="28%"/>
<img src="/media/admin_02.png" width="28%"/>
<img src="/media/admin_03.png" width="28%"/>
</p>
<p>
<img src="/media/admin_04.png" width="28%"/>
<img src="/media/admin_05.png" width="28%"/>
<img src="/media/admin_06.png" width="28%"/>
</p>
<p>
<img src="/media/admin_07.png" width="28%"/>
<img src="/media/admin_08.png" width="28%"/>
<img src="/media/admin_09.png" width="28%"/>
</p>
<p>
<img src="/media/admin_10.png" width="28%"/>
<img src="/media/admin_11.png" width="28%"/>
<img src="/media/admin_12.png" width="28%"/>
</p>
<p>
<img src="/media/admin_13.png" width="28%"/>
<img src="/media/admin_14.png" width="28%"/>
<img src="/media/admin_15.png" width="28%"/>
</p>


## License

Copyright 2022 Riley Farro

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

