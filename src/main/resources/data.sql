INSERT INTO items (title, description, img_path, price) VALUES
 ('Мяч', 'Футбольный мяч', 'images/ball.jpg', 1000),
 ('Кроссовки', 'Беговые кроссовки', 'images/sneakers.jpg', 5000),
 ('Футболка', 'Спортивная футболка', 'images/tshirt.jpg', 1500)
ON CONFLICT (title) DO NOTHING;